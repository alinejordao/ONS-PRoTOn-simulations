package ra.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testes conceituais da Failure Centrality.
 *
 * Teste 1:
 * valida T1 no caso deterministico.
 *
 * Teste 2:
 * valida Rv no caso:
 *
 * P1 FAILED = [1, 2, 3]
 * P2 FAILED = [2, 4]
 *
 * link 1 = KNOWN_WORKING
 * link 2 = UNKNOWN
 * link 3 = UNKNOWN
 * link 4 = UNKNOWN
 *
 * Resultado esperado:
 *
 * Rv(link 2) = 2 / 3
 */
public class FailureCentralityCalculatorTest {

 public static void main(String[] args) {

    FailureCentralityCalculator calculator =
            new FailureCentralityCalculator();

    testT1(calculator);
    testRv(calculator);
    testT2(calculator);
    testFailureCentrality(calculator);
}

    /**
     * Teste conceitual do T1.
     */
    private static void testT1(
            FailureCentralityCalculator calculator) {

        System.out.println();
        System.out.println("========== FC TEST T1 ==========");

        int e1 = 1;
        int e2 = 2;
        int e3 = 3;

        Map<Integer, ComponentState> linkStates =
                new HashMap<>();

        linkStates.put(e1, ComponentState.KNOWN_WORKING);
        linkStates.put(e2, ComponentState.UNKNOWN);
        linkStates.put(e3, ComponentState.KNOWN_WORKING);

        int[] pathNodes = new int[] {10, 20, 30, 40};
        int[] pathLinks = new int[] {e1, e2, e3};

        Probe failedProbe = new Probe(
                10,
                40,
                pathNodes,
                pathLinks,
                1
        );

        failedProbe.setResult(ProbeResult.FAILED);

        Observation failedObservation =
                new Observation(
                        failedProbe,
                        ProbeResult.FAILED,
                        new int[0],
                        pathLinks,
                        1
                );

        List<Observation> observations =
                new ArrayList<>();

        observations.add(failedObservation);

        System.out.println("[FC-TEST-T1] Before:");
        System.out.println(
                "[FC-TEST-T1] link "
                + e1 + " = "
                + linkStates.get(e1)
        );

        System.out.println(
                "[FC-TEST-T1] link "
                + e2 + " = "
                + linkStates.get(e2)
        );

        System.out.println(
                "[FC-TEST-T1] link "
                + e3 + " = "
                + linkStates.get(e3)
        );

        double t1 = calculator.calculateT1(
                e2,
                linkStates,
                observations
        );

        Map<Integer, Double> centrality =
                calculator.calculateDeterministic(
                        linkStates,
                        observations
                );

        System.out.println(
                "[FC-TEST-T1] T1(link "
                + e2 + ") = "
                + t1
        );

        System.out.println(
                "[FC-TEST-T1] After: link "
                + e2 + " = "
                + linkStates.get(e2)
        );

        System.out.println(
                "[FC-TEST-T1] centrality(link "
                + e2 + ") = "
                + centrality.get(e2)
        );

        boolean passed =
                t1 == 1.0
                && linkStates.get(e2)
                    == ComponentState.KNOWN_FAILED
                && centrality.get(e2) == 1.0;

        if (passed) {
            System.out.println(
                    "[FC-TEST-T1] TEST PASSED"
            );
        } else {
            System.out.println(
                    "[FC-TEST-T1] TEST FAILED"
            );
        }
    }

    /**
     * Teste conceitual do Rv.
     *
     * P1 = [e1, e2, e3]
     * P2 = [e2, e4]
     *
     * e1 = KNOWN_WORKING
     *
     * Depois de remover e1:
     *
     * P1 = [e2, e3]
     * P2 = [e2, e4]
     *
     * Uniao = {e2, e3, e4}
     *
     * Rv(e2) = 2 / 3
     */
    private static void testRv(
            FailureCentralityCalculator calculator) {

        System.out.println();
        System.out.println("========== FC TEST RV ==========");

        int e1 = 1;
        int e2 = 2;
        int e3 = 3;
        int e4 = 4;

        Map<Integer, ComponentState> linkStates =
                new HashMap<>();

        linkStates.put(e1, ComponentState.KNOWN_WORKING);
        linkStates.put(e2, ComponentState.UNKNOWN);
        linkStates.put(e3, ComponentState.UNKNOWN);
        linkStates.put(e4, ComponentState.UNKNOWN);

        List<Observation> observations =
                new ArrayList<>();

        /*
         * P1 FAILED = [e1, e2, e3]
         */
        int[] pathNodes1 =
                new int[] {10, 20, 30, 40};

        int[] pathLinks1 =
                new int[] {e1, e2, e3};

        Probe probe1 = new Probe(
                10,
                40,
                pathNodes1,
                pathLinks1,
                1
        );

        probe1.setResult(ProbeResult.FAILED);

        Observation observation1 =
                new Observation(
                        probe1,
                        ProbeResult.FAILED,
                        new int[0],
                        pathLinks1,
                        1
                );

        observations.add(observation1);

        /*
         * P2 FAILED = [e2, e4]
         */
        int[] pathNodes2 =
                new int[] {20, 30, 50};

        int[] pathLinks2 =
                new int[] {e2, e4};

        Probe probe2 = new Probe(
                20,
                50,
                pathNodes2,
                pathLinks2,
                1
        );

        probe2.setResult(ProbeResult.FAILED);

        Observation observation2 =
                new Observation(
                        probe2,
                        ProbeResult.FAILED,
                        new int[0],
                        pathLinks2,
                        1
                );

        observations.add(observation2);

        double rv = calculator.calculateRv(
                e2,
                linkStates,
                observations
        );

        double expectedRv = 2.0 / 3.0;

        System.out.println(
                "[FC-TEST-RV] Rv(link "
                + e2 + ") = "
                + rv
        );

        System.out.println(
                "[FC-TEST-RV] Expected = "
                + expectedRv
        );

        /*
         * Evitamos comparar double com igualdade exata.
         */
        boolean passed =
                Math.abs(rv - expectedRv) < 0.000001;

        if (passed) {
            System.out.println(
                    "[FC-TEST-RV] TEST PASSED"
            );
        } else {
            System.out.println(
                    "[FC-TEST-RV] TEST FAILED"
            );
        }
    }
    /**
 * Teste conceitual do T2.
 *
 * Valores controlados:
 *
 * Rv = 2 / 3
 * prior = 0.1
 * k = 10
 *
 * Formula:
 *
 * T2 = prior
 *      + (1 - prior)
 *      * (1 - 1 / (Rv + 1)^(k * prior))
 *
 * Resultado esperado:
 *
 * T2 = 0.46
 */
private static void testT2(
        FailureCentralityCalculator calculator) {

    System.out.println();
    System.out.println("========== FC TEST T2 ==========");

    double rv = 2.0 / 3.0;
    double prior = 0.1;
    double k = 10.0;

    double t2 = calculator.calculateT2(
            rv,
            prior,
            k
    );
    
    

    double expectedT2 = 0.46;

    System.out.println(
            "[FC-TEST-T2] Rv = "
            + rv
    );

    System.out.println(
            "[FC-TEST-T2] prior = "
            + prior
    );

    System.out.println(
            "[FC-TEST-T2] k = "
            + k
    );

    System.out.println(
            "[FC-TEST-T2] T2 = "
            + t2
    );

    System.out.println(
            "[FC-TEST-T2] Expected = "
            + expectedT2
    );

    boolean passed =
            Math.abs(t2 - expectedT2) < 0.000001;

    if (passed) {
        System.out.println(
                "[FC-TEST-T2] TEST PASSED"
        );
    } else {
        System.out.println(
                "[FC-TEST-T2] TEST FAILED"
        );
    }
}

/**
 * Teste conceitual da Failure Centrality completa.
 *
 * Caso A:
 * T1 = 1 -> FC deve ser 1.0
 *
 * Caso B:
 * T1 = 0 e T2 = 0.46 -> FC deve ser 0.46
 */
private static void testFailureCentrality(
        FailureCentralityCalculator calculator) {

    System.out.println();
    System.out.println("========== FC COMPLETE TEST ==========");

    /*
     * =========================================================
     * CASO A - FALHA DETERMINISTICA
     * =========================================================
     *
     * FAILED path = [e1, e2, e3]
     *
     * e1 = KNOWN_WORKING
     * e2 = UNKNOWN
     * e3 = KNOWN_WORKING
     *
     * Depois da remocao dos WORKING:
     *
     * [e2]
     *
     * Portanto:
     *
     * T1(e2) = 1
     * FC(e2) = 1
     */
    int e1 = 1;
    int e2 = 2;
    int e3 = 3;

    Map<Integer, ComponentState> statesA =
            new HashMap<>();

    statesA.put(e1, ComponentState.KNOWN_WORKING);
    statesA.put(e2, ComponentState.UNKNOWN);
    statesA.put(e3, ComponentState.KNOWN_WORKING);

    int[] nodesA =
            new int[] {10, 20, 30, 40};

    int[] linksA =
            new int[] {e1, e2, e3};

    Probe probeA = new Probe(
            10,
            40,
            nodesA,
            linksA,
            1
    );

    probeA.setResult(ProbeResult.FAILED);

    Observation observationA =
            new Observation(
                    probeA,
                    ProbeResult.FAILED,
                    new int[0],
                    linksA,
                    1
            );

    List<Observation> observationsA =
            new ArrayList<>();

    observationsA.add(observationA);

    double prior = 0.1;
    double k = 10.0;

    double fcA =
            calculator.calculateFailureCentrality(
                    e2,
                    statesA,
                    observationsA,
                    prior,
                    k
            );

    double expectedA = 1.0;

    System.out.println(
            "[FC-COMPLETE-A] FC(link 2) = "
            + fcA
    );

    System.out.println(
            "[FC-COMPLETE-A] Expected = "
            + expectedA
    );

    boolean passedA =
            Math.abs(fcA - expectedA) < 0.000001
            && statesA.get(e2)
                == ComponentState.KNOWN_FAILED;

    if (passedA) {
        System.out.println(
                "[FC-COMPLETE-A] TEST PASSED"
        );
    } else {
        System.out.println(
                "[FC-COMPLETE-A] TEST FAILED"
        );
    }


    /*
     * =========================================================
     * CASO B - SUSPEITA SEM CERTEZA
     * =========================================================
     *
     * P1 FAILED = [e1, e2, e3]
     * P2 FAILED = [e2, e4]
     *
     * e1 = KNOWN_WORKING
     *
     * Depois da remocao:
     *
     * P1 = [e2, e3]
     * P2 = [e2, e4]
     *
     * T1(e2) = 0
     * Rv(e2) = 2/3
     *
     * prior = 0.1
     * k = 10
     *
     * T2 = 0.46
     *
     * Portanto:
     *
     * FC = max(0, 0.46)
     * FC = 0.46
     *
     * e2 deve continuar UNKNOWN.
     */
    int b1 = 11;
    int b2 = 12;
    int b3 = 13;
    int b4 = 14;

    Map<Integer, ComponentState> statesB =
            new HashMap<>();

    statesB.put(b1, ComponentState.KNOWN_WORKING);
    statesB.put(b2, ComponentState.UNKNOWN);
    statesB.put(b3, ComponentState.UNKNOWN);
    statesB.put(b4, ComponentState.UNKNOWN);

    List<Observation> observationsB =
            new ArrayList<>();

    int[] nodesB1 =
            new int[] {100, 200, 300, 400};

    int[] linksB1 =
            new int[] {b1, b2, b3};

    Probe probeB1 = new Probe(
            100,
            400,
            nodesB1,
            linksB1,
            1
    );

    probeB1.setResult(ProbeResult.FAILED);

    Observation observationB1 =
            new Observation(
                    probeB1,
                    ProbeResult.FAILED,
                    new int[0],
                    linksB1,
                    1
            );

    observationsB.add(observationB1);

    int[] nodesB2 =
            new int[] {200, 300, 500};

    int[] linksB2 =
            new int[] {b2, b4};

    Probe probeB2 = new Probe(
            200,
            500,
            nodesB2,
            linksB2,
            1
    );

    probeB2.setResult(ProbeResult.FAILED);

    Observation observationB2 =
            new Observation(
                    probeB2,
                    ProbeResult.FAILED,
                    new int[0],
                    linksB2,
                    1
            );

    observationsB.add(observationB2);

    double fcB =
            calculator.calculateFailureCentrality(
                    b2,
                    statesB,
                    observationsB,
                    prior,
                    k
            );

    double expectedB = 0.46;

    System.out.println(
            "[FC-COMPLETE-B] FC(link 12) = "
            + fcB
    );

    System.out.println(
            "[FC-COMPLETE-B] Expected = "
            + expectedB
    );

    System.out.println(
            "[FC-COMPLETE-B] State = "
            + statesB.get(b2)
    );

    boolean passedB =
            Math.abs(fcB - expectedB) < 0.000001
            && statesB.get(b2)
                == ComponentState.UNKNOWN;

    if (passedB) {
        System.out.println(
                "[FC-COMPLETE-B] TEST PASSED"
        );
    } else {
        System.out.println(
                "[FC-COMPLETE-B] TEST FAILED"
        );
    }


    /*
     * Resultado geral.
     */
    if (passedA && passedB) {
        System.out.println(
                "[FC-COMPLETE] ALL TESTS PASSED"
        );
    } else {
        System.out.println(
                "[FC-COMPLETE] TEST FAILED"
        );
    }
}

}