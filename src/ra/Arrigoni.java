package ons.ra;

import ons.*;
import ons.util.WeightedGraph;
import ons.util.YenKSP;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

/**
 * RA baseado em:
 * "Tomography-based progressive network recovery and critical service restoration
 * after massive failures" (Arrigoni et al., INFOCOM 2023).
 *
 */
public class Arrigoni implements RA {

    private ControlPlaneForRA cp;
    private WeightedGraph fullGraph; // grafo original pré-falha
    private WeightedGraph knownGraph; // visão progressiva do algoritmo
    

    @Override
    public void simulationInterface(ControlPlaneForRA cp) {
        this.cp = cp;
        
        this.fullGraph = cp.getPT().getWeightedGraph();
    }

    /* ========= 1. Roteamento normal ========= */

    @Override
    public void flowArrival(Flow flow) {
        // Por enquanto, está KSP simples (1 caminho) como roteamento baseline.
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
        // Nada por enquanto.
    }

    /* ========= 2. Entrada de desastre ========= */

    @Override
    public void disasterArrival(DisasterArea area) {

        // 1) Construir grafo pós-falha
        WeightedGraph postGraph = computeTomographicGraph(cp.getPT(), area);
        
        this.knownGraph = postGraph;
        System.out.println("[B6] knownGraph inicializado com grafo pós-falha.");

        // 2) Obter fluxos interrompidos
        List<Flow> interrupted = new ArrayList<>(cp.getInteruptedFlows());
         System.out.println("Interrupted flows: " + interrupted.size());
         System.out.println("Esta vendo os fluxos interrompidos");
        // 3) Separar críticos / não críticos (aqui está simples, tudo crítico)
        List<Flow> criticalFlows = selectCriticalFlows(interrupted);
        List<Flow> nonCriticalFlows = selectNonCriticalFlows(interrupted);

        // 4) Aplicar recuperação progressiva
        progressiveRecovery(postGraph, criticalFlows, nonCriticalFlows);
    }

    @Override
    public void disasterDeparture() {
        // Pode ficar vazio nesta versão inicial.
    }

    /* ========= 3. Flows atrasados (opcional) ========= */

    @Override
    public void delayedFlowDeparture(Flow f) { }

    @Override
    public void delayedFlowArrival(Flow f) {
        // Para já: tentar re-rotear de novo no grafo atual
        int K = 1;
        WeightedGraph postGraph = computeTomographicGraph(cp.getPT(), null);
        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                postGraph, f.getSource(), f.getDestination(), K);
        boolean restored = tryRerouteAndRestore(f, paths);
        if (!restored) {
            cp.dropFlow(f);
            f.updateTransmittedBw();
        }
    }

    /* ========= 4. Constrói um grafo pós-falha ========= */

    /**
     * Aqui:
     *  - Links interrompidos recebem peso infinito (inutilizáveis).
     *  - Demais links mantêm o peso original.
     *  Depois enriquecer com “tomografia” (usando ocupação, etc.).
     */
    private WeightedGraph computeTomographicGraph(PhysicalTopology pt, DisasterArea area) {
    int nodes = pt.getNumNodes();
    WeightedGraph g = new WeightedGraph(nodes);

    for (int i = 0; i < nodes; i++) {
        for (int j = 0; j < nodes; j++) {
            if (pt.hasLink(i, j)) {
                EONLink link = (EONLink) pt.getLink(i, j);

                // Link interrompido não entra no grafo pós-falha.
                // Assim o KSP não tenta usar esse enlace.
                if (!link.isIsInterupted()) {
                    g.addEdge(i, j, link.getWeight());
                }
            }
        }
    }

    return g;
}

    /* ========= 5. Seleção de fluxos ========= */

    private List<Flow> selectCriticalFlows(List<Flow> interrupted) {
        List<Flow> critical = new ArrayList<>();
        for (Flow f : interrupted) {
            // todo mundo é crítico
            // Depois usar f.getServiceInfo().getServiceInfo() para filtrar
            critical.add(f);
        }
        return critical;
    }

    private List<Flow> selectNonCriticalFlows(List<Flow> interrupted) {
        // Por enquanto, nenhum não-crítico
        return new ArrayList<>();
    }

    /* ========= 6. Recuperação progressiva ========= */

    private void progressiveRecovery(WeightedGraph postGraph,
                                     List<Flow> criticalFlows,
                                     List<Flow> nonCriticalFlows) {

        // 1) (Opcional) degradar flows sobreviventes para liberar recursos

        // 2) Restaurar críticos
        restoreCriticalFlows(postGraph, criticalFlows);

        // 3) Restaurar não críticos (aqui vazio, por enquanto)
        restoreNonCriticalFlows(postGraph, nonCriticalFlows);
        
    }
    
    /*"A score-based prioritization mechanism was introduced, 
    selecting flows based on a benefit-to-cost ratio, 
    approximating PRoTOn’s decision strategy."*/
    
    private double computeScore(Flow f, WeightedGraph g) {

    double benefit = f.getBwReq();

    ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
            g, f.getSource(), f.getDestination(), 1);

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

        // proxy de disponibilidade
        slotAvailability += ((EONLink) cp.getPT().getLink(linkId))
                .getNumFreeSlots();
    }

    if (cost == 0) return 0;

    // normalizações simples
    double normCost = 1.0 / cost;
    double normHops = 1.0 / (hops + 1);
    double normSlots = slotAvailability;

    // pesos (ajustáveis!)
    double alpha = 0.5;
    double beta = 0.3;
    double gamma = 0.2;

    double score =
            alpha * (benefit * normCost) +
            beta * normHops +
            gamma * normSlots;

    return score;
}
    
    private void restoreCriticalFlows(WeightedGraph postGraph, List<Flow> criticalFlows) {
    int K = 10;

    Collections.sort(criticalFlows, (f1, f2) ->
            Integer.compare(f2.getBwReq(), f1.getBwReq())
    );

    List<Flow> pending = new ArrayList<>(criticalFlows);

    int iteration = 0;
    int maxIterations = 10;

    while (!pending.isEmpty() && iteration < maxIterations) {
        iteration++;

        System.out.println("[C1] Iteracao progressiva=" + iteration
                + " Pendentes=" + pending.size());

        boolean recoveredInThisIteration = false;

        pending.sort((f1, f2) -> {
            double score1 = computeScore(f1, knownGraph);
            double score2 = computeScore(f2, knownGraph);
            return Double.compare(score2, score1);
        });

        Iterator<Flow> it = pending.iterator();

        while (it.hasNext()) {
            Flow f = it.next();

            System.out.println("[C1] Tentando restaurar fluxo ID=" + f.getID()
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
                System.out.println("[C1] Fluxo restaurado ID=" + f.getID());
                it.remove();
                recoveredInThisIteration = true;
            } else {
                System.out.println("[C1] Fluxo nao restaurado nesta iteracao ID=" + f.getID());
            }
        }

        if (!recoveredInThisIteration) {
            System.out.println("[C1] Nenhum fluxo restaurado nesta iteracao. Encerrando loop progressivo.");
            break;
        }
    }

    for (Flow f : pending) {
        System.out.println("[C1] Fluxo nao recuperado apos iteracoes -> dropFlow ID=" + f.getID());
        cp.dropFlow(f);
        f.updateTransmittedBw();
    }
}

    private void restoreNonCriticalFlows(WeightedGraph postGraph, List<Flow> nonCriticalFlows) {
        // Futuro: mesma lógica que críticos, mas com política mais “best effort”
    }

    /* ========= 7. Re-roteamento e restauro ========= */

    /**
     * Tenta (re)rotear um fluxo usando caminhos candidatos.
     * Retorna true se conseguiu aceitar/restaurar, false caso contrário.
     */
    
    // tryRerouteAndRestore só para chegadas novas 
    private boolean tryRerouteAndRestore(Flow f, ArrayList<Integer>[] paths) {

        if (paths == null) return false;

        long id;
        LightPath[] lps = new LightPath[1];

        OUTER:
        for (ArrayList<Integer> path : paths) {
            if (path == null || path.isEmpty()) continue;

            int[] nodes = convertIntegers(path);
            if (nodes.length == 0) continue;

            int[] links = new int[nodes.length - 1];
            for (int j = 0; j < nodes.length - 1; j++) {
                links[j] = cp.getPT().getLink(nodes[j], nodes[j + 1]).getID();
            }

            // tamanho da rota
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
            if (requiredSlots >= 100000) continue;

            // checar capacidade em todos os links
            for (int linkId : links) {
                if (!((EONLink) cp.getPT().getLink(linkId)).hasSlotsAvaiable(requiredSlots)) {
                    continue OUTER;
                }
            }

            // First-Fit: tentar slots do primeiro link
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

                    // aceitando (para chegada normal) ou restaurando (para interrompido)
                    // aqui o acceptFlow, para fora de desastre; dentro de disasterArrival
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

    /**
 * Tenta re-rotear e RESTAURAR um fluxo interrompido.
 * Usa rerouteFlow + restoreFlow.
 */
private boolean tryRecovery(Flow f, ArrayList<Integer>[] paths) {
    if (paths == null || paths.length == 0) {
        System.out.println(" Nenhum caminho candidato para fluxo ID=" + f.getID());
        return false;
    }

    long id;
    LightPath[] lps = new LightPath[1];

    OUTER:
    for (ArrayList<Integer> path : paths) {
        System.out.println(" Caminho candidato para fluxo ID=" + f.getID() + ": " + path);

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

        System.out.print(" Links da rota para fluxo ID=" + f.getID() + ": ");
        for (int linkId : links) {
            System.out.print(linkId + " ");
        }
        System.out.println();

        double sizeRoute = 0;

        for (int linkId : links) {
            sizeRoute += cp.getPT().getLink(linkId).getWeight();
        }

        int modulation = Modulation.getBestModulation(sizeRoute);

        if (modulation < 0) {
        System.out.println(" Nenhuma modulacao suporta oficialmente a rota do fluxo ID="
            + f.getID()
            + " rotaSize=" + sizeRoute
            + ". Usando BPSK como fallback experimental.");

         modulation = 0;
        }

        f.setModulation(modulation);

       int requiredSlots = Modulation.convertRateToSlot(
        f.getBwReq(),
        EONPhysicalTopology.getSlotSize(),
        modulation
        );

        if (requiredSlots >= 100000) {
            System.out.println(" Modulação inválida para fluxo ID=" + f.getID()
                    + " rotaSize=" + sizeRoute
                    + " requiredSlots=" + requiredSlots);
            continue;
        }

        System.out.println(" Fluxo ID=" + f.getID()
                + " rotaSize=" + sizeRoute
                + " modulation=" + modulation
                + " requiredSlots=" + requiredSlots);

        for (int linkId : links) {
            if (!((EONLink) cp.getPT().getLink(linkId)).hasSlotsAvaiable(requiredSlots)) {
                System.out.println(" Sem slots no link ID=" + linkId
                        + " para fluxo ID=" + f.getID()
                        + " requiredSlots=" + requiredSlots);
                continue OUTER;
            }
        }

        int[] firstSlot = ((EONLink) cp.getPT().getLink(links[0]))
                .getSlotsAvailableToArray(requiredSlots);

        System.out.println(" Quantidade de slots candidatos no primeiro link: "
                + firstSlot.length);

        /*"A bounded slot-allocation search strategy was introduced
        to avoid exhaustive spectrum scanning improving computational efficiency 
        and aligning with progressive recovery behavior."*/
        
        int maxAttempts = 20;
        int attempts = 0;

        for (int slot : firstSlot) {

    if (attempts >= maxAttempts) {
        System.out.println(" Limite de tentativas atingido para fluxo ID=" + f.getID());
        break;
    }

    attempts++;
    
    
    double failureProbability = 0.2; // 20% de incerteza simulada

        if (Math.random() < failureProbability) {
        System.out.println("[B7] Falha simulada na rota para fluxo ID=" + f.getID()
            + " no slot inicial=" + slot);
        continue;
    }
    
            System.out.println(" Tentando criar lightpath no slot inicial=" + slot
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

                System.out.println(" Lightpath criado ID=" + id
                        + " para fluxo ID=" + f.getID());

                if (cp.acceptFlow(f.getID(), lps)) {
                    System.out.println(" Flow aceito novamente ID=" + f.getID());

                    cp.restoreFlow(f);
                    updateKnownGraph(links);
                return true;
                    } else {
                    System.out.println(" AcceptFlow recusou fluxo ID=" + f.getID()
                      + ". Desalocando lightpath ID=" + id);

    cp.getVT().deallocatedLightpath(id);
}
            } else {
                System.out.println("Falha ao criar lightpath para fluxo ID=" + f.getID()
                        + " no slot inicial=" + slot);
            }
        }
    }

    return false;
}

/*"A progressive knowledge graph was introduced, 
where successfully restored paths are incorporated into the algorithm’s 
view of the network, enabling adaptive recovery 
decisions similar to PRoTOn."*/

    private void updateKnownGraph(int[] links) {

    double learningProbability = 0.6; // aprende 60% dos links da rota

    System.out.println("[C3] Atualizando knownGraph parcialmente");

    for (int linkId : links) {

        if (Math.random() > learningProbability) {
            System.out.println("[C3] Link nao aprendido nesta rodada: ID=" + linkId);
            continue;
        }

        int src = cp.getPT().getLink(linkId).getSource();
        int dst = cp.getPT().getLink(linkId).getDestination();
        double weight = cp.getPT().getLink(linkId).getWeight();

        knownGraph.addEdge(src, dst, weight);

        System.out.println("[C3] Link aprendido: " + src + " -> " + dst);
    }
}

    /* ========= 8. Utilitário: converter lista para array ========= */

    private int[] convertIntegers(ArrayList<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        return ret;
    }
    private void debugNodeDegree(WeightedGraph g, int src, int dst) {
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

    System.out.println(" Grau no grafo pos-falha: src=" + src
            + " degree=" + srcDegree
            + " | dst=" + dst
            + " degree=" + dstDegree);
}
}