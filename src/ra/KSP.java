package ons.ra;

import ons.*;
import ons.util.WeightedGraph;
import ons.util.YenKSP;

import java.util.ArrayList;

/**
 * RA simples: K caminhos mais curtos + First-Fit, sem desastres,
 * sem upgrade e sem degradação. Serve para qualquer topologia.
 */
public class KSP implements RA {

    private ControlPlaneForRA cp;
    private WeightedGraph graph;

    @Override
    public void simulationInterface(ControlPlaneForRA cp) {
        this.cp = cp;
        this.graph = cp.getPT().getWeightedGraph();
    }

    @Override
    public void flowArrival(Flow flow) {

        int K = 3; // número de caminhos candidatos

        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(
                graph,
                flow.getSource(),
                flow.getDestination(),
                K
        );
        flow.setPaths(paths);

        long id;
        LightPath[] lps = new LightPath[1];

        for (ArrayList<Integer> path : paths) {

            if (path == null || path.isEmpty()) {
                continue;
            }

            int[] nodes = convertIntegers(path);
            if (nodes.length == 0) {
                continue;
            }

            // vetor de links
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
            flow.setModulation(modulation);

            int requiredSlots = Modulation.convertRateToSlot(
                    flow.getBwReq(),
                    EONPhysicalTopology.getSlotSize(),
                    modulation
            );
            if (requiredSlots >= 100000) {
                continue;
            }

            // checa capacidade em todos os links
            boolean ok = true;
            for (int linkId : links) {
                if (!((EONLink) cp.getPT().getLink(linkId)).hasSlotsAvaiable(requiredSlots)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }

            // First-Fit
            int[] firstSlot = ((EONLink) cp.getPT().getLink(links[0]))
                    .getSlotsAvailableToArray(requiredSlots);

            for (int slot : firstSlot) {
                EONLightPath lp = cp.createCandidateEONLightPath(
                        flow.getSource(),
                        flow.getDestination(),
                        links,
                        slot,
                        slot + requiredSlots - 1,
                        modulation
                );

                if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                    lps[0] = cp.getVT().getLightpath(id);
                    if (cp.acceptFlow(flow.getID(), lps)) {
                        return;
                    } else {
                        cp.getVT().deallocatedLightpath(id);
                    }
                }
            }
        }

        // não encontrou caminho viável
        cp.blockFlow(flow.getID());
    }

    @Override
    public void flowDeparture(long id) {
        // nada especial
    }

    @Override
    public void disasterArrival(DisasterArea area) {
        // este RA ignora desastres
    }

    @Override
    public void disasterDeparture() {
        // este RA ignora desastres
    }

    @Override
    public void delayedFlowDeparture(Flow f) {
        // não usamos atraso neste RA simples
    }

    @Override
    public void delayedFlowArrival(Flow f) {
        // se algum fluxo atrasado chegar aqui, simplesmente tenta de novo
        // como uma chegada normal; se não quiser isso, pode só bloquear:
        flowArrival(f);
    }

    // conversão auxiliar
    private int[] convertIntegers(ArrayList<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        return ret;
    }
}