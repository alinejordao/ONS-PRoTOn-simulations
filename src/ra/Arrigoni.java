package ons.ra;

import ons.*;
import ons.util.WeightedGraph;
import ons.util.YenKSP;

import java.util.ArrayList;
import java.util.List;

/**
 * RA baseado em:
 * "Tomography-based progressive network recovery and critical service restoration
 * after massive failures" (Arrigoni et al., INFOCOM 2023).
 *
 */
public class Arrigoni implements RA {

    private ControlPlaneForRA cp;
    private WeightedGraph fullGraph; // grafo original pré-falha

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

                    if (link.isIsInterupted()) {
                        // link quebrado: não deve ser usado
                        g.addEdge(i, j, Integer.MAX_VALUE);
                    } else {
                        // por enquanto, peso = weight original
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

   private void restoreCriticalFlows(WeightedGraph postGraph, List<Flow> criticalFlows) {
    int K = 3;
    for (Flow f : criticalFlows) {
        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                postGraph, f.getSource(), f.getDestination(), K);

        boolean recovered = tryRecovery(f, paths);

        if (!recovered) {
            // não conseguiu restaurar → dropa
            cp.dropFlow(f);
            f.updateTransmittedBw();
        }
        // se recovered == true,
        // rerouteFlow + restoreFlow já foram chamados dentro de tryRecovery
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

                // Aqui usamos rerouteFlow, pois o fluxo já existia
                if (cp.rerouteFlow(f.getID(), lps)) {
                    // e marcamos explicitamente como restaurado
                    cp.restoreFlow(f);
                    return true;
                } else {
                    cp.getVT().deallocatedLightpath(id);
                }
            }
        }
    }

    return false;
}

    /* ========= 8. Utilitário: converter lista para array ========= */

    private int[] convertIntegers(ArrayList<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        return ret;
    }
}
