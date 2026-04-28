package ons.ra;

import ons.*;
import ons.util.WeightedGraph;
import ons.util.YenKSP;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aline
 * 
 * RA baseado em:
 * "Tomography-based progressive network recovery and critical service restoration
 * after massive failures", IEEE INFOCOM 2023 (Arrigoni et al.).
 */
public class Arrigoni implements RA {

    private ControlPlaneForRA cp;
    private WeightedGraph fullGraph;        // grafo original (pré-falha)

    @Override
    public void simulationInterface(ControlPlaneForRA cp) {
        this.cp = cp;
        this.fullGraph = cp.getPT().getWeightedGraph();
    }

    /* =========================
       1. Rotas em condições normais
       ========================= */

    @Override
    public void flowArrival(Flow flow) {
        // Roteamento/alocação “baseline”.
        // Você pode começar copiando algo tipo KSP/First-Fit
        // ou uma versão simplificada do EON_QFDDM_*.
    }

    @Override
    public void flowDeparture(long id) {
        // Nenhum tratamento especial necessário neste momento.
    }

    /* =========================
       2. Entrada de desastre
       ========================= */

    @Override
    public void disasterArrival(DisasterArea area) {

        // 1) Construir visão pós-falha (grafo tomográfico aproximado)
        WeightedGraph postGraph = computeTomographicGraph(cp.getPT(), area);

        // 2) Selecionar fluxos interrompidos e classificá-los
        List<Flow> interrupted = new ArrayList<>(cp.getInteruptedFlows());
        List<Flow> criticalFlows   = selectCriticalFlows(interrupted);
        List<Flow> nonCriticalFlows = selectNonCriticalFlows(interrupted);

        // 3) Aplicar esquema de recuperação progressiva:
        //    - possivelmente degradar alguns flows sobreviventes
        //    - restaurar críticos em ondas
        progressiveRecovery(postGraph, criticalFlows, nonCriticalFlows);
    }

    @Override
    public void disasterDeparture() {
        // Se o modelo do paper tiver alguma ação no "fim" do desastre, trate aqui.
    }

    /* =========================
       3. Flows atrasados (se usar)
       ========================= */

    @Override
    public void delayedFlowDeparture(Flow f) {
        // Se você usar flows atrasados (delay tolerant), trate aqui.
    }

    @Override
    public void delayedFlowArrival(Flow f) {
        // Pode reutilizar a lógica de restauro progressivo para flows atrasados.
        // Por exemplo, tentar upgrade/re-roteamento novamente depois de algum tempo.
    }

    /* =========================
       4. Métodos auxiliares – “tomography-based recovery”
       ========================= */

    /**
     * Constrói um grafo pós-falha, possivelmente incorporando
     * informação tomográfica (pesos/capacidades inferidos).
     */
    private WeightedGraph computeTomographicGraph(PhysicalTopology pt, DisasterArea area) {
        int nodes = pt.getNumNodes();
        WeightedGraph g = new WeightedGraph(nodes);

        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) {
                if (pt.hasLink(i, j)) {
                    EONLink link = (EONLink) pt.getLink(i, j);

                    if (link.isIsInterupted()) {
                        // Link totalmente indisponível pós-falha
                        g.addEdge(i, j, Integer.MAX_VALUE);
                    } else {
                        // Aqui você pode ajustar o peso conforme “tomografia”:
                        // - uso de banda
                        // - latência estimada
                        // - etc.
                        g.addEdge(i, j, link.getWeight());
                    }
                }
            }
        }
        return g;
    }

    /**
     * Seleciona os fluxos considerados críticos (por exemplo,
     * classes de serviço mais altas, menor tolerância a atraso/degradação).
     */
    private List<Flow> selectCriticalFlows(List<Flow> interrupted) {
        List<Flow> critical = new ArrayList<>();
        for (Flow f : interrupted) {
            ServiceInfo si = f.getServiceInfo();
            int serviceClass = si.getServiceInfo();

            // Exemplo: classes 0 e 1 são críticas
            if (serviceClass == 0 || serviceClass == 1) {
                critical.add(f);
            }
        }
        return critical;
    }

    /**
     * Demais fluxos interrompidos, não críticos.
     */
    private List<Flow> selectNonCriticalFlows(List<Flow> interrupted) {
        List<Flow> nonCritical = new ArrayList<>();
        for (Flow f : interrupted) {
            ServiceInfo si = f.getServiceInfo();
            int serviceClass = si.getServiceInfo();

            if (!(serviceClass == 0 || serviceClass == 1)) {
                nonCritical.add(f);
            }
        }
        return nonCritical;
    }

    /**
     * Aplica a lógica de recuperação progressiva:
     *  - possivelmente degradar flows sobreviventes para liberar recursos;
     *  - restaurar flows críticos com prioridade;
     *  - tratar flows não críticos de forma best-effort.
     */
    private void progressiveRecovery(WeightedGraph postGraph,
                                     List<Flow> criticalFlows,
                                     List<Flow> nonCriticalFlows) {

        // 1) Opcional: degradar alguns flows sobreviventes (não interrompidos)
        //    para liberar capacidade, conforme política do paper.
        // degradeSurvivedFlowsForCapacity(postGraph);

        // 2) Restaurar críticos em ordem de prioridade (classe de serviço, SLA, etc.)
        restoreCriticalFlows(postGraph, criticalFlows);

        // 3) Tentar restaurar não críticos (best-effort)
        restoreNonCriticalFlows(postGraph, nonCriticalFlows);
    }

    /**
     * Restaura fluxos críticos usando o grafo pós-falha:
     *  - computa K rotas,
     *  - tenta alocar espectro,
     *  - se conseguir, cp.restoreFlow(f); senão, pode atrasar ou dropar.
     */
    private void restoreCriticalFlows(WeightedGraph postGraph, List<Flow> criticalFlows) {
        int K = 3; // ou o que fizer sentido

        for (Flow f : criticalFlows) {
            // Exemplo: tente re-rotear como na flowArrival, mas usando postGraph
            ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                    postGraph, f.getSource(), f.getDestination(), K);

            boolean restored = tryRerouteAndRestore(f, paths);
            if (!restored) {
                // Política do paper: atrasar, degradar mais, ou dropar
                cp.dropFlow(f);
                f.updateTransmittedBw();
            }
        }
    }

    /**
     * Restauração best-effort para fluxos não críticos.
     */
    private void restoreNonCriticalFlows(WeightedGraph postGraph, List<Flow> nonCriticalFlows) {
        int K = 3;

        for (Flow f : nonCriticalFlows) {
            ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                    postGraph, f.getSource(), f.getDestination(), K);

            boolean restored = tryRerouteAndRestore(f, paths);
            if (!restored) {
                // Menos prioridade: provavelmente apenas drop
                cp.dropFlow(f);
                f.updateTransmittedBw();
            }
        }
    }

    /**
     * Tenta re-rotear um fluxo usando um conjunto de caminhos candidatos
     * e criar um novo lightpath + restauro.
     */
    private boolean tryRerouteAndRestore(Flow f, ArrayList<Integer>[] paths) {
        // Aqui você pode reutilizar o padrão que já existe em EON_QFDDM_RESMF:
        // - converter caminho em links
        // - escolher modulação
        // - converter rate em slots
        // - tentar First-Fit
        // - se conseguir: cp.restoreFlow(f) ou cp.upgradeFlow(f, lps)
        return false; // placeholder
    }
}
