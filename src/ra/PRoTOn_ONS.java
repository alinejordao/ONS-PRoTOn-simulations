package ons.ra;

import ons.*;
import ons.util.WeightedGraph;
import ons.util.YenKSP;

import ra.monitoring.ComponentState;
import ra.monitoring.Probe;
import ra.monitoring.Observation;
import ra.monitoring.ProbeResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

/**
 * Algoritmo RA inspirado no PRoTOn:
 * "Tomography-based progressive network recovery and critical service restoration
 * after massive failures" (Arrigoni et al.).
 *
 * Esta versão implementa uma adaptação para o simulador ONS, considerando:
 *  - Roteamento normal com KSP;
 *  - Construção de grafo pós-falha;
 *  - Recuperação progressiva de fluxos interrompidos;
 *  - Priorização por score benefício/custo;
 *  - Reprovisionamento de fluxos via acceptFlow;
 *  - Busca limitada por slots;
 *  - Incerteza simulada, aproximando o comportamento de tomografia;
 *  - Aprendizado parcial do grafo conhecido (knownGraph).
 */
public class PRoTOn_ONS implements RA {

    private ControlPlaneForRA cp;

    // Grafo original da topologia física antes da falha.
    private WeightedGraph fullGraph;

    // Grafo conhecido pelo algoritmo durante a recuperação progressiva.
    // Ele representa a visão parcial/evolutiva da rede após o desastre.
    private WeightedGraph knownGraph;

    // Estado físico real da rede após o desastre.
    // Será usado como referência para o aprendizado progressivo do knownGraph.
    private WeightedGraph postFailureGraph;
    
    // Estado conhecido dos enlaces pelo algoritmo de monitoramento.
    private Map<Integer, ComponentState> linkStates;

    // Probes realizados durante a recuperacao.
    private List<Probe> probes;

    // Observacoes produzidas pelos probes.
    private List<Observation> observations;
    
 
   @Override
    public void simulationInterface(ControlPlaneForRA cp) {
    this.cp = cp;
    this.fullGraph = cp.getPT().getWeightedGraph();

    this.linkStates = new HashMap<>();
    this.probes = new ArrayList<>();
    this.observations = new ArrayList<>();
    }
    
    
 /* ============================================================
 * MONITORING - ESTADO E OBSERVACAO DA REDE
 * ============================================================ */
    
    /**
 * Inicializa o estado de conhecimento dos enlaces da topologia.
 *
 * Neste momento, todos os enlaces começam como UNKNOWN.
 * O estado representa o conhecimento do algoritmo sobre o componente,
 * e não o estado físico real da rede.
 */
private void initializeComponentStates() {

    linkStates.clear();

    PhysicalTopology pt = cp.getPT();

    int numNodes = pt.getNumNodes();
    int totalLinks = 0;

    for (int i = 0; i < numNodes; i++) {
        for (int j = 0; j < numNodes; j++) {

            Link link = pt.getLink(i, j);

            if (link != null) {
                linkStates.put(link.getID(), ComponentState.UNKNOWN);
                totalLinks++;
            }
        }
    }

    System.out.println(
            "[MONITORING] Component states initialized: "
            + totalLinks
            + " links marked as UNKNOWN."
    );
}
    /**
 * Executa um probe sobre o estado fisico real da rede.
 *
 * O algoritmo conhece apenas o resultado final WORKING/FAILED.
 * A verificacao enlace a enlace contra o postFailureGraph representa
 * apenas o "oraculo" interno da simulacao.
 */
private ProbeResult executeProbe(Probe probe) {

    int[] pathNodes = probe.getPathNodes();

    if (pathNodes == null || pathNodes.length < 2) {
        probe.setResult(ProbeResult.FAILED);
        return ProbeResult.FAILED;
    }

    for (int i = 0; i < pathNodes.length - 1; i++) {

        int src = pathNodes[i];
        int dst = pathNodes[i + 1];

        if (!postFailureGraph.isEdge(src, dst)) {
            probe.setResult(ProbeResult.FAILED);
            return ProbeResult.FAILED;
        }
    }

    probe.setResult(ProbeResult.WORKING);
    return ProbeResult.WORKING;
}

/**
 * Converte o resultado de um probe em uma observacao.
 *
 * WORKING:
 * todos os enlaces do caminho sao confirmados como operacionais.
 *
 * FAILED:
 * os enlaces ainda desconhecidos do caminho sao registrados como
 * pertencentes a um failed probe path, sem marca-los individualmente
 * como KNOWN_FAILED.
 */
private Observation createObservation(Probe probe) {

    ProbeResult result = probe.getResult();
    int[] pathLinks = probe.getPathLinks();

    if (result == ProbeResult.WORKING) {

        int[] confirmed = pathLinks.clone();

        for (int linkId : confirmed) {
            linkStates.put(linkId, ComponentState.KNOWN_WORKING);
        }

        return new Observation(
                probe,
                result,
                confirmed,
                new int[0],
                probe.getIteration()
        );
    }

    ArrayList<Integer> unknownLinks = new ArrayList<>();

    for (int linkId : pathLinks) {

        ComponentState state = linkStates.get(linkId);

        if (state == ComponentState.UNKNOWN) {
            unknownLinks.add(linkId);
        }
    }

    int[] failedPathLinks = new int[unknownLinks.size()];

    for (int i = 0; i < unknownLinks.size(); i++) {
        failedPathLinks[i] = unknownLinks.get(i);
    }

    return new Observation(
            probe,
            result,
            new int[0],
            failedPathLinks,
            probe.getIteration()
    );
}
    

/**
 * Executa um probe de teste usando o primeiro fluxo interrompido.
 *
 * Metodo temporario para validacao da infraestrutura de monitoramento.
 */
private void runInitialMonitoringTest(List<Flow> interrupted) {

    if (interrupted == null || interrupted.isEmpty()) {
        System.out.println("[MONITORING] Nenhum fluxo interrompido para teste de probe.");
        return;
    }

    Flow f = interrupted.get(0);

    ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
            fullGraph,
            f.getSource(),
            f.getDestination(),
            1
    );

    if (paths == null
            || paths.length == 0
            || paths[0] == null
            || paths[0].isEmpty()) {

        System.out.println("[MONITORING] Nenhum caminho encontrado para probe de teste.");
        return;
    }

    int[] nodes = convertIntegers(paths[0]);
    int[] links = new int[nodes.length - 1];

    for (int i = 0; i < nodes.length - 1; i++) {
        links[i] = cp.getPT()
                .getLink(nodes[i], nodes[i + 1])
                .getID();
    }

    Probe probe = new Probe(
            f.getSource(),
            f.getDestination(),
            nodes,
            links,
            0
    );

    probes.add(probe);

    ProbeResult result = executeProbe(probe);

    Observation observation = createObservation(probe);
    observations.add(observation);

    System.out.println(
            "[MONITORING] Probe "
            + probe.getSourceMonitor()
            + " -> "
            + probe.getDestinationMonitor()
            + " result="
            + result
            + " links="
            + links.length
    );

    System.out.println(
            "[MONITORING] Confirmed working links="
            + observation.getWorkingLinksConfirmed().length
            + " | Failed path links="
            + observation.getFailedPathLinks().length
    );
}
    



/**
 * Executa um probe de teste em um enlace confirmado como operacional
 * no postFailureGraph.
 *
 * Metodo temporario usado apenas para validar o comportamento WORKING.
 */
private void runWorkingProbeTest() {

    PhysicalTopology pt = cp.getPT();
    int numNodes = pt.getNumNodes();

    for (int i = 0; i < numNodes; i++) {
        for (int j = 0; j < numNodes; j++) {

            if (postFailureGraph.isEdge(i, j)) {

                Link link = pt.getLink(i, j);

                if (link == null) {
                    continue;
                }

                int[] nodes = new int[] {i, j};
                int[] links = new int[] {link.getID()};

                Probe probe = new Probe(
                        i,
                        j,
                        nodes,
                        links,
                        0
                );

                probes.add(probe);

                ProbeResult result = executeProbe(probe);

                Observation observation = createObservation(probe);
                observations.add(observation);

                ComponentState state = linkStates.get(link.getID());

                System.out.println(
                        "[MONITORING-TEST] Working probe "
                        + i + " -> " + j
                        + " | link=" + link.getID()
                        + " | result=" + result
                        + " | state=" + state
                );

                return;
            }
        }
    }

    System.out.println(
            "[MONITORING-TEST] Nenhum enlace operacional encontrado para teste."
    );
}


    /* ============================================================
     * ETAPA 01 - ROTEAMENTO NORMAL DE FLUXOS
     * ============================================================ */

    @Override
    public void flowArrival(Flow flow) {
       
        // Roteamento baseline para fluxos novos: usa KSP com K = 1.
        int K = 1;

        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                fullGraph,
                flow.getSource(),
                flow.getDestination(),
                K
        );

        if (paths == null || paths.length == 0 || paths[0] == null) {
            cp.blockFlow(flow.getID());
            return;
        }

        boolean accepted = tryRerouteAndRestore(flow, paths);

        if (!accepted) {
            cp.blockFlow(flow.getID());
        }
    }

    @Override
    public void flowDeparture(long id) {
        // Nenhuma ação específica nesta versão.
    }

    /* ============================================================
     * ETAPA 02 - ENTRADA DO DESASTRE E PREPARAÇÃO DA RECUPERAÇÃO
     * ============================================================ */

    @Override
    public void disasterArrival(DisasterArea area) {

    System.out.println("[ETAPA 02] Desastre detectado. Preparando recuperacao progressiva.");

    // Constrói o grafo pós-falha removendo enlaces interrompidos.
    this.postFailureGraph = buildPostFailureGraph(cp.getPT());

    // Inicializa o estado conhecido dos enlaces para o mecanismo de monitoramento.
    // Neste momento, todos os enlaces começam como UNKNOWN.
    initializeComponentStates();

    // Inicializa o grafo conhecido com o estado pós-falha inicial.
    // Depois, ele será atualizado progressivamente conforme caminhos forem restaurados.
    this.knownGraph = buildInitialKnownGraph(cp.getPT());

    // Obtém os fluxos interrompidos pelo desastre.
    List<Flow> interrupted = new ArrayList<>(cp.getInteruptedFlows());
    System.out.println("[ETAPA 02] Fluxos interrompidos: " + interrupted.size());
    
    //// Executa o teste inicial do mecanismo de monitoramento.
    runInitialMonitoringTest(interrupted);
    runWorkingProbeTest();

    // Separa fluxos críticos e não críticos.
    // Nesta versão inicial, todos os fluxos interrompidos são tratados como críticos.
    List<Flow> criticalFlows = selectCriticalFlows(interrupted);
    List<Flow> nonCriticalFlows = selectNonCriticalFlows(interrupted);

    // Aplica a recuperação progressiva.
    progressiveRecovery(this.postFailureGraph, criticalFlows, nonCriticalFlows);
    
    
    
    
}

    @Override
    public void disasterDeparture() {
        // Nenhuma ação específica nesta versão.
    }

    /* ============================================================
     * ETAPA 03 - TRATAMENTO DE FLUXOS ATRASADOS
     * ============================================================ */

    @Override
    public void delayedFlowDeparture(Flow f) {
        // Nenhuma ação específica nesta versão.
    }

    @Override
    public void delayedFlowArrival(Flow f) {
        System.out.println("[ETAPA 03] Tratando fluxo atrasado ID=" + f.getID());

        // Tenta reacomodar fluxos atrasados usando o grafo pós-falha atual.
        int K = 1;
        WeightedGraph postGraph = buildPostFailureGraph(cp.getPT());

        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                postGraph,
                f.getSource(),
                f.getDestination(),
                K
        );

        boolean restored = tryRerouteAndRestore(f, paths);

        if (!restored) {
            cp.dropFlow(f);
            f.updateTransmittedBw();
        }
    }

    /* ============================================================
     * ETAPA 04 - CONSTRUÇÃO DO GRAFO PÓS-FALHA
     * ============================================================ */

    /**
     * Constrói um grafo pós-falha contendo apenas enlaces disponíveis.
     *
     * Diferença importante em relação a uma implementação ingênua:
     *  - Enlaces interrompidos NÃO são adicionados ao grafo.
     *  - Isso evita que o KSP tente usar enlaces quebrados com peso infinito.
     */
    private WeightedGraph buildPostFailureGraph(PhysicalTopology pt) {
        System.out.println("[ETAPA 04] Construindo grafo pos-falha.");

        int nodes = pt.getNumNodes();
        WeightedGraph g = new WeightedGraph(nodes);

        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) {
                if (pt.hasLink(i, j)) {
                    EONLink link = (EONLink) pt.getLink(i, j);

                    if (!link.isIsInterupted()) {
                        g.addEdge(i, j, link.getWeight());
                    }
                }
            }
        }

        return g;
    }
    
    
    /**
 * Constrói a visão inicial parcial da rede conhecida pelo algoritmo.
 *
 * O grafo físico pós-falha representa o estado real da rede.
 * O knownGraph contém inicialmente apenas uma fração dos enlaces
 * operacionais, simulando conhecimento incompleto após a falha.
 */
private WeightedGraph buildInitialKnownGraph(PhysicalTopology pt) {

    System.out.println("[ETAPA 04] Construindo visao inicial parcial do knownGraph.");

    int nodes = pt.getNumNodes();
    WeightedGraph g = new WeightedGraph(nodes);

    double initialKnowledgeProbability = 0.9;

    int operationalLinks = 0;
    int knownLinks = 0;

    for (int i = 0; i < nodes; i++) {
        for (int j = 0; j < nodes; j++) {

            if (pt.hasLink(i, j)) {

                EONLink link = (EONLink) pt.getLink(i, j);

                if (!link.isIsInterupted()) {

                    operationalLinks++;

                    if (Math.random() < initialKnowledgeProbability) {
                        g.addEdge(i, j, link.getWeight());
                        knownLinks++;
                    }
                }
            }
        }
    }

    System.out.println(
            "[ETAPA 04] knownGraph parcial construido: "
            + knownLinks
            + "/"
            + operationalLinks
            + " enlaces operacionais conhecidos."
    );

    return g;
}

/* ============================================================
 * ETAPA 09 - EXPANSÃO PROGRESSIVA DO GRAFO CONHECIDO
 * ============================================================ */

/**
 * Expande gradualmente o conhecimento da rede.
 *
 * Procura enlaces que estão operacionais no grafo físico pós-falha,
 * mas ainda não fazem parte do knownGraph.
 */
private boolean expandKnownGraph() {

    System.out.println("[ETAPA 09] Expandindo progressivamente o knownGraph...");

    double discoveryProbability = 0.10;
    boolean learnedSomething = false;

    int nodes = cp.getPT().getNumNodes();

    for (int i = 0; i < nodes; i++) {
        for (int j = 0; j < nodes; j++) {

            // O enlace existe e está operacional no estado real pós-falha,
            // mas ainda não é conhecido pelo algoritmo.
            if (postFailureGraph.isEdge(i, j)
                    && !knownGraph.isEdge(i, j)) {

                if (Math.random() < discoveryProbability) {

                    double weight = cp.getPT().getLink(i, j).getWeight();

                    knownGraph.addEdge(i, j, weight);

                    learnedSomething = true;

                    System.out.println(
                            "[ETAPA 09] Novo enlace descoberto: "
                            + i + " -> " + j
                    );
                }
            }
        }
    }

    return learnedSomething;
}


    /* ============================================================
     * ETAPA 05 - CLASSIFICAÇÃO DE FLUXOS
     * ============================================================ */

    private List<Flow> selectCriticalFlows(List<Flow> interrupted) {
        System.out.println("[ETAPA 05] Classificando fluxos interrompidos.");

        List<Flow> critical = new ArrayList<>();

        for (Flow f : interrupted) {
            // Nesta versão, todos os fluxos interrompidos são críticos.
            // Futuramente, pode-se filtrar por classe de serviço, SLA ou prioridade.
            critical.add(f);
        }

        return critical;
    }

    private List<Flow> selectNonCriticalFlows(List<Flow> interrupted) {
        // Nesta versão, nenhum fluxo é tratado como não crítico.
        return new ArrayList<>();
    }

    /* ============================================================
     * ETAPA 06 - RECUPERAÇÃO PROGRESSIVA
     * ============================================================ */

    private void progressiveRecovery(WeightedGraph postGraph,
                                     List<Flow> criticalFlows,
                                     List<Flow> nonCriticalFlows) {

        System.out.println("[ETAPA 06] Iniciando recuperacao progressiva.");

        // Primeiro restaura fluxos críticos.
        restoreCriticalFlows(postGraph, criticalFlows);

        // Futuramente, fluxos não críticos podem ser restaurados em política best effort.
        restoreNonCriticalFlows(postGraph, nonCriticalFlows);
    }

    /**
     * Calcula um score para priorização dos fluxos.
     *
     * O score combina:
     *  - benefício: banda requisitada pelo fluxo;
     *  - custo: distância/peso do menor caminho;
     *  - número de hops;
     *  - disponibilidade de slots ao longo da rota.
     *
     * Essa função aproxima a ideia PRoTOn-like de priorizar recuperações
     * com melhor relação benefício/custo.
     */
    private double computeScore(Flow f, WeightedGraph g) {
        double benefit = f.getBwReq();

        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                g,
                f.getSource(),
                f.getDestination(),
                1
        );

        if (paths == null || paths.length == 0 || paths[0] == null) {
            return 0;
        }

        int[] nodes = convertIntegers(paths[0]);

        double cost = 0;
        int hops = nodes.length - 1;
        double slotAvailability = 0;

        for (int i = 0; i < nodes.length - 1; i++) {
            int linkId = cp.getPT().getLink(nodes[i], nodes[i + 1]).getID();

            cost += cp.getPT().getLink(linkId).getWeight();

            // Proxy de disponibilidade espectral: soma dos slots livres nos enlaces da rota.
            slotAvailability += ((EONLink) cp.getPT().getLink(linkId)).getAvaiableSlots();
        }

        if (cost == 0) {
            return 0;
        }

        // Normalizações simples.
        double normCost = 1.0 / cost;
        double normHops = 1.0 / (hops + 1);
        double normSlots = slotAvailability;

        // Pesos ajustáveis para composição do score.
        double alpha = 0.5; // peso do benefício/custo
        double beta = 0.3;  // peso do número de hops
        double gamma = 0.2; // peso da disponibilidade de slots

        return alpha * (benefit * normCost)
                + beta * normHops
                + gamma * normSlots;
    }

    /**
     * Restaura fluxos críticos de forma progressiva.
     *
     * A recuperação ocorre em múltiplas iterações:
     *  1. ordena fluxos pendentes por score;
     *  2. tenta restaurar cada fluxo usando o knownGraph;
     *  3. fluxos restaurados são removidos da lista de pendentes;
     *  4. se nenhuma restauração ocorrer em uma iteração, o processo encerra.
     */
    private void restoreCriticalFlows(WeightedGraph postGraph, List<Flow> criticalFlows) {
        int K = 10;

        // Ordenação inicial simples por maior banda.
        Collections.sort(criticalFlows, (f1, f2) ->
                Integer.compare(f2.getBwReq(), f1.getBwReq())
        );

        List<Flow> pending = new ArrayList<>(criticalFlows);

        int iteration = 0;
        int maxIterations = 10;

        while (!pending.isEmpty() && iteration < maxIterations) {
            iteration++;

            System.out.println("[ETAPA 06] Iteracao progressiva=" + iteration
                    + " | Pendentes=" + pending.size());

            boolean recoveredInThisIteration = false;

            // Ordenação dinâmica por score PRoTOn-like.
            pending.sort((f1, f2) -> {
                double score1 = computeScore(f1, knownGraph);
                double score2 = computeScore(f2, knownGraph);
                return Double.compare(score2, score1);
            });

            Iterator<Flow> it = pending.iterator();

            while (it.hasNext()) {
                Flow f = it.next();

                System.out.println("[ETAPA 06] Tentando restaurar fluxo ID=" + f.getID()
                        + " src=" + f.getSource()
                        + " dst=" + f.getDestination()
                        + " bw=" + f.getBwReq());

                debugNodeDegree(knownGraph, f.getSource(), f.getDestination());

                ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                        knownGraph,
                        f.getSource(),
                        f.getDestination(),
                        K
                );

                boolean recovered = tryRecovery(f, paths);

                if (recovered) {
                    System.out.println("[ETAPA 06] Fluxo restaurado ID=" + f.getID());
                    it.remove();
                    recoveredInThisIteration = true;
                } else {
                    System.out.println("[ETAPA 06] Fluxo nao restaurado nesta iteracao ID=" + f.getID());
                }
            }

          if (!recoveredInThisIteration) {

    boolean learnedSomething = expandKnownGraph();

    if (learnedSomething) {
        System.out.println(
                "[ETAPA 06] Nenhuma restauracao, mas novos enlaces foram descobertos. "
                + "Iniciando nova iteracao."
        );
        continue;
    }

    System.out.println(
            "[ETAPA 06] Nenhum fluxo restaurado e nenhum novo enlace descoberto. "
            + "Encerrando recuperacao progressiva."
    );

    break;
}
        }

        // Fluxos que permanecerem pendentes são descartados.
        for (Flow f : pending) {
            System.out.println("[ETAPA 06] Fluxo nao recuperado apos iteracoes -> dropFlow ID=" + f.getID());
            cp.dropFlow(f);
            f.updateTransmittedBw();
        }
    }

    private void restoreNonCriticalFlows(WeightedGraph postGraph, List<Flow> nonCriticalFlows) {
        // Futuro: aplicar lógica semelhante aos críticos, mas com menor prioridade.
    }

    /* ============================================================
     * ETAPA 07 - ROTEAMENTO DE FLUXOS NOVOS
     * ============================================================ */

    /**
     * Tenta alocar um fluxo novo usando caminhos candidatos.
     * Usado para chegadas normais, fora do contexto de recuperação de desastre.
     */
    private boolean tryRerouteAndRestore(Flow f, ArrayList<Integer>[] paths) {
        

        if (paths == null) {
            return false;
        }

        long id;
        LightPath[] lps = new LightPath[1];

        OUTER:
        for (ArrayList<Integer> path : paths) {
            if (path == null || path.isEmpty()) {
                continue;
            }

            int[] nodes = convertIntegers(path);
            if (nodes.length == 0) {
                continue;
            }

            int[] links = new int[nodes.length - 1];
            for (int j = 0; j < nodes.length - 1; j++) {
                links[j] = cp.getPT().getLink(nodes[j], nodes[j + 1]).getID();
            }

            double sizeRoute = 0;
            for (int linkId : links) {
                sizeRoute += cp.getPT().getLink(linkId).getWeight();
            }

            int modulation = Modulation.getBestModulation(sizeRoute);
            f.setModulation(modulation);

            int requiredSlots = Modulation.convertRateToSlot(
                    f.getBwReq(),
                    EONPhysicalTopology.getSlotSize(),
                    modulation
            );

            if (requiredSlots >= 100000) {
                continue;
            }

            for (int linkId : links) {
                if (!((EONLink) cp.getPT().getLink(linkId)).hasSlotsAvaiable(requiredSlots)) {
                    continue OUTER;
                }
            }

            int[] firstSlot = ((EONLink) cp.getPT().getLink(links[0]))
                    .getSlotsAvailableToArray(requiredSlots);

            for (int slot : firstSlot) {
                EONLightPath lp = cp.createCandidateEONLightPath(
                        f.getSource(),
                        f.getDestination(),
                        links,
                        slot,
                        slot + requiredSlots - 1,
                        modulation
                );

                if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                    lps[0] = cp.getVT().getLightpath(id);

                    if (cp.acceptFlow(f.getID(), lps)) {
                        return true;
                    } else {
                        cp.getVT().deallocatedLightpath(id);
                    }
                }
            }
        }

        return false;
    }

    /* ============================================================
     * ETAPA 08 - RECUPERAÇÃO DE FLUXOS INTERROMPIDOS
     * ============================================================ */

    /**
     * Tenta recuperar um fluxo interrompido.
     *
     * Importante:
     *  - Usa acceptFlow em vez de rerouteFlow, pois o fluxo interrompido pode
     *    não estar mais em estado interno adequado para reroute no ONS.
     *  - Aplica limite de tentativas por caminho.
     *  - Aplica incerteza simulada para aproximar a ideia de tomografia.
     *  - Ao restaurar, atualiza parcialmente o knownGraph.
     */
    private boolean tryRecovery(Flow f, ArrayList<Integer>[] paths) {
        System.out.println("[ETAPA 08] Iniciando tentativa de recuperacao do fluxo ID=" + f.getID());

        if (paths == null || paths.length == 0) {
            System.out.println("[ETAPA 08] Nenhum caminho candidato para fluxo ID=" + f.getID());
            return false;
        }

        long id;
        LightPath[] lps = new LightPath[1];

        OUTER:
        for (ArrayList<Integer> path : paths) {
            System.out.println("[ETAPA 08] Caminho candidato para fluxo ID=" + f.getID() + ": " + path);

            if (path == null || path.isEmpty()) {
                continue;
            }

            int[] nodes = convertIntegers(path);
            if (nodes.length == 0) {
                continue;
            }

            int[] links = new int[nodes.length - 1];
            for (int j = 0; j < nodes.length - 1; j++) {
                links[j] = cp.getPT().getLink(nodes[j], nodes[j + 1]).getID();
            }

            System.out.print("[ETAPA 08] Links da rota para fluxo ID=" + f.getID() + ": ");
            for (int linkId : links) {
                System.out.print(linkId + " ");
            }
            System.out.println();

            double sizeRoute = 0;
            for (int linkId : links) {
                sizeRoute += cp.getPT().getLink(linkId).getWeight();
            }

            int modulation = Modulation.getBestModulation(sizeRoute);

            
             // Se nenhuma modulação suportar oficialmente a rota,
             // este caminho é descartado e o algoritmo tenta o próximo candidato.
            if (modulation < 0) {
                 System.out.println("[ETAPA 08] Rota descartada: nenhuma modulacao suporta oficialmente o fluxo ID="
                 + f.getID()
                 + " rotaSize=" + sizeRoute);
             continue;
            }

            f.setModulation(modulation);

            int requiredSlots = Modulation.convertRateToSlot(
                    f.getBwReq(),
                    EONPhysicalTopology.getSlotSize(),
                    modulation
            );

            if (requiredSlots >= 100000) {
                System.out.println("[ETAPA 08] Modulacao invalida para fluxo ID=" + f.getID()
                        + " rotaSize=" + sizeRoute
                        + " requiredSlots=" + requiredSlots);
                continue;
            }

            System.out.println("[ETAPA 08] Fluxo ID=" + f.getID()
                    + " rotaSize=" + sizeRoute
                    + " modulation=" + modulation
                    + " requiredSlots=" + requiredSlots);

            for (int linkId : links) {
                if (!((EONLink) cp.getPT().getLink(linkId)).hasSlotsAvaiable(requiredSlots)) {
                    System.out.println("[ETAPA 08] Sem slots no link ID=" + linkId
                            + " para fluxo ID=" + f.getID()
                            + " requiredSlots=" + requiredSlots);
                    continue OUTER;
                }
            }

            int[] firstSlot = ((EONLink) cp.getPT().getLink(links[0]))
                    .getSlotsAvailableToArray(requiredSlots);

            System.out.println("[ETAPA 08] Quantidade de slots candidatos no primeiro link: " + firstSlot.length);
            
            // Incerteza simulada no nível do caminho, não no nível do slot.
            // Representa incerteza sobre o estado dos enlaces da rota.
            double pathUncertaintyProbability = 0.2;

            if (Math.random() < pathUncertaintyProbability) {
               System.out.println("[ETAPA 08] Incerteza simulada: caminho descartado para fluxo ID="
                 + f.getID());
            continue;
            }

            // Limita a busca por slots para evitar varredura exaustiva do espectro.
            int maxAttempts = 20;
            int attempts = 0;

            for (int slot : firstSlot) {
                if (attempts >= maxAttempts) {
                    System.out.println("[ETAPA 08] Limite de tentativas atingido para fluxo ID=" + f.getID());
                    break;
                }

                attempts++;



                System.out.println("[ETAPA 08] Tentando criar lightpath no slot inicial=" + slot
                        + " slotFinal=" + (slot + requiredSlots - 1));

                EONLightPath lp = cp.createCandidateEONLightPath(
                        f.getSource(),
                        f.getDestination(),
                        links,
                        slot,
                        slot + requiredSlots - 1,
                        modulation
                );

                if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                    lps[0] = cp.getVT().getLightpath(id);

                    System.out.println("[ETAPA 08] Lightpath criado ID=" + id
                            + " para fluxo ID=" + f.getID());

                    // Reprovisiona o fluxo interrompido.
                    if (cp.acceptFlow(f.getID(), lps)) {
                        System.out.println("[ETAPA 08] Fluxo aceito novamente ID=" + f.getID());

                     if (isInterruptedFlow(f)) {
                 cp.restoreFlow(f);
                    System.out.println("[ETAPA 08] Fluxo marcado como restaurado ID=" + f.getID());
                    }

                    return true;
                    } else {
                        System.out.println("[ETAPA 08] acceptFlow recusou fluxo ID=" + f.getID()
                                + ". Desalocando lightpath ID=" + id);
                        cp.getVT().deallocatedLightpath(id);
                    }
                } else {
                    System.out.println("[ETAPA 08] Falha ao criar lightpath para fluxo ID=" + f.getID()
                            + " no slot inicial=" + slot);
                }
            }
        }

        return false;
        
    }

    /* ============================================================
     * ETAPA 10 - MÉTODOS AUXILIARES
     * ============================================================ */

    private int[] convertIntegers(ArrayList<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        return ret;
    }

    /**
     * Mostra o grau de origem e destino no grafo conhecido.
     * Ajuda a diagnosticar se o fluxo está sem caminho por isolamento de nó.
     */
    private void debugNodeDegree(WeightedGraph g, int src, int dst) {
        System.out.println("[ETAPA 10] Executando diagnostico de conectividade no knownGraph.");

        int srcDegree = 0;
        int dstDegree = 0;

        for (int i = 0; i < g.size(); i++) {
            if (g.isEdge(src, i)) {
                srcDegree++;
            }
            if (g.isEdge(dst, i)) {
                dstDegree++;
            }
        }

        System.out.println("[ETAPA 10] Grau no knownGraph: src=" + src
                + " degree=" + srcDegree
                + " | dst=" + dst
                + " degree=" + dstDegree);
    }
        private boolean isInterruptedFlow(Flow f) {
    for (Flow interrupted : cp.getInteruptedFlows()) {
        if (interrupted.getID() == f.getID()) {
            return true;
        }
    }
    return false;
}
}
