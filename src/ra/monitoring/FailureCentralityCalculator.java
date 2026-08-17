package ra.monitoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula a Failure Centrality dos enlaces a partir das observacoes
 * produzidas pelos probes.
 *
 * Nesta etapa esta implementado apenas o termo T1.
 *
 * A adaptacao considera somente enlaces, pois esta primeira versao
 * do PRoTOn_ONS-FC trabalha com falhas de enlaces da topologia EON.
 */
public class FailureCentralityCalculator {

    /**
     * Calcula T1 para um enlace especifico.
     *
     * T1 = 1 quando existe pelo menos um failed path que:
     *
     *  - contem o enlace analisado;
     *  - apos remover os enlaces KNOWN_WORKING;
     *  - possui somente um componente restante.
     *
     * Nesse caso, o enlace restante e identificado como falho
     * com certeza.
     *
     * Caso contrario, T1 = 0.
     */
    public double calculateT1(
            int linkId,
            Map<Integer, ComponentState> linkStates,
            List<Observation> observations) {

        /*
         * Um enlace comprovadamente operacional nunca pode
         * apresentar Failure Centrality de falha.
         */
        ComponentState currentState = linkStates.get(linkId);

        if (currentState == ComponentState.KNOWN_WORKING) {
            return 0.0;
        }

        int failedPathsContainingLink = 0;
        int isolatedPaths = 0;

        for (Observation observation : observations) {

            if (observation.getResult() != ProbeResult.FAILED) {
                continue;
            }

            int[] failedPathLinks = observation.getFailedPathLinks();

            if (failedPathLinks == null || failedPathLinks.length == 0) {
                continue;
            }

            /*
             * Primeiro verificamos se o caminho falho
             * contem o enlace analisado.
             */
            boolean containsLink = false;

            for (int pathLinkId : failedPathLinks) {
                if (pathLinkId == linkId) {
                    containsLink = true;
                    break;
                }
            }

            if (!containsLink) {
                continue;
            }

            failedPathsContainingLink++;

            /*
             * Equivalente a:
             *
             * p \ E_WORKING
             *
             * da formulacao original.
             *
             * Removemos SOMENTE enlaces conhecidos como WORKING.
             */
            int remainingElements = 0;

            for (int pathLinkId : failedPathLinks) {

                ComponentState state = linkStates.get(pathLinkId);

                if (state != ComponentState.KNOWN_WORKING) {
                    remainingElements++;
                }
            }

            /*
             * floor(1 / |p \ E_WORKING|)
             *
             * e igual a 1 somente quando resta exatamente
             * um componente no failed path.
             */
            if (remainingElements == 1) {
                isolatedPaths++;
            }
        }

        /*
         * Se o enlace nao aparece em nenhum failed path,
         * T1 nao fornece evidencia de falha.
         *
         * O tratamento com prior sera feito posteriormente
         * na Failure Centrality completa.
         */
        if (failedPathsContainingLink == 0) {
            return 0.0;
        }

        /*
         * Equivale conceitualmente ao:
         *
         * ceil(
         *     sum(floor(1 / tamanho_do_caminho_reduzido))
         *     / numero_de_failed_paths
         * )
         *
         * Como cada termo vale apenas 0 ou 1,
         * basta existir pelo menos um caminho isolando
         * o componente para T1 = 1.
         */
        if (isolatedPaths > 0) {
            return 1.0;
        }

        return 0.0;
    }

    
    
    /**
 * Calcula Rv para um enlace especifico.
 *
 * Adaptacao link-centric da Failure Centrality para o PRoTOn_ONS-FC.
 *
 * Rv representa a concentracao de evidencias de falha sobre um enlace:
 *
 *              numero de failed paths contendo o enlace
 * Rv = --------------------------------------------------------
 *      numero de enlaces distintos ainda incertos na uniao
 *      desses failed paths
 *
 * Enlaces KNOWN_WORKING sao removidos antes da construcao da uniao,
 * pois ja foram confirmados como operacionais pelos probes.
 *
 * @param linkId       enlace analisado
 * @param linkStates   estado conhecido dos enlaces
 * @param observations observacoes produzidas pelos probes
 *
 * @return valor de Rv para o enlace
 */
public double calculateRv(
        int linkId,
        Map<Integer, ComponentState> linkStates,
        List<Observation> observations) {

    /*
     * Enlaces comprovadamente operacionais nao possuem
     * evidencia de falha.
     */
    ComponentState currentState = linkStates.get(linkId);

    if (currentState == ComponentState.KNOWN_WORKING) {
        return 0.0;
    }

    int failedPathsContainingLink = 0;

    /*
     * Guarda os IDs distintos dos enlaces que permanecem
     * nos failed paths depois da remocao dos KNOWN_WORKING.
     */
    java.util.Set<Integer> uncertainLinks = new java.util.HashSet<>();

    for (Observation observation : observations) {

        if (observation.getResult() != ProbeResult.FAILED) {
            continue;
        }

        int[] failedPathLinks = observation.getFailedPathLinks();

        if (failedPathLinks == null || failedPathLinks.length == 0) {
            continue;
        }

        /*
         * Verifica se este failed path contem o enlace
         * cuja centralidade esta sendo analisada.
         */
        boolean containsLink = false;

        for (int pathLinkId : failedPathLinks) {
            if (pathLinkId == linkId) {
                containsLink = true;
                break;
            }
        }

        if (!containsLink) {
            continue;
        }

        failedPathsContainingLink++;

        /*
         * Adiciona a uniao somente os enlaces que ainda
         * nao foram confirmados como WORKING.
         *
         * O Set elimina automaticamente duplicatas.
         */
        for (int pathLinkId : failedPathLinks) {

            ComponentState state = linkStates.get(pathLinkId);

            if (state != ComponentState.KNOWN_WORKING) {
                uncertainLinks.add(pathLinkId);
            }
        }
    }

    /*
     * O enlace nao participa de nenhum failed path.
     * Rv nao fornece evidencia adicional.
     *
     * O prior sera tratado posteriormente no calculo de T2/FC.
     */
    if (failedPathsContainingLink == 0 || uncertainLinks.isEmpty()) {
        return 0.0;
    }

    return (double) failedPathsContainingLink
            / (double) uncertainLinks.size();
}


/**
 * Calcula o termo T2 da Failure Centrality.
 *
 * T2 combina:
 *
 *  - prior: probabilidade a priori de falha do componente;
 *  - Rv: concentracao das evidencias dos failed paths;
 *  - k: parametro de ajuste da funcao.
 *
 * Formula adaptada da Failure Centrality utilizada no PROTON:
 *
 * T2 = prior + (1 - prior)
 *      * (1 - 1 / (Rv + 1)^(k * prior))
 *
 * @param rv    valor de Rv do enlace
 * @param prior probabilidade a priori de falha
 * @param k     parametro de ajuste
 *
 * @return valor de T2
 */
public double calculateT2(
        double rv,
        double prior,
        double k) {

    if (prior < 0.0 || prior > 1.0) {
        throw new IllegalArgumentException(
                "Prior must be between 0 and 1."
        );
    }

    if (k <= 0.0) {
        throw new IllegalArgumentException(
                "k must be greater than zero."
        );
    }

    if (rv < 0.0) {
        throw new IllegalArgumentException(
                "Rv cannot be negative."
        );
    }

    return prior
            + (1.0 - prior)
            * (
                1.0
                - 1.0
                / Math.pow(
                        rv + 1.0,
                        k * prior
                )
            );
}

/**
 * Calcula a Failure Centrality completa para um enlace.
 *
 * Regras:
 *
 * 1. KNOWN_WORKING -> FC = 0.0
 * 2. KNOWN_FAILED  -> FC = 1.0
 * 3. Se o enlace nao aparece em nenhum failed path -> FC = prior
 * 4. Caso contrario:
 *
 *      FC = max(T1, T2)
 *
 * onde:
 *
 *      T1 = evidencia deterministica
 *      Rv = concentracao das evidencias de falha
 *      T2 = combinacao entre prior e Rv
 *
 * @param linkId       enlace analisado
 * @param linkStates   estados conhecidos dos enlaces
 * @param observations observacoes produzidas pelos probes
 * @param prior        probabilidade a priori de falha
 * @param k            parametro de ajuste do T2
 *
 * @return Failure Centrality do enlace
 */
public double calculateFailureCentrality(
        int linkId,
        Map<Integer, ComponentState> linkStates,
        List<Observation> observations,
        double prior,
        double k) {

    ComponentState state = linkStates.get(linkId);

    /*
     * Casos conhecidos com certeza.
     */
    if (state == ComponentState.KNOWN_WORKING) {
        return 0.0;
    }

    if (state == ComponentState.KNOWN_FAILED) {
        return 1.0;
    }

    /*
     * Verifica se o enlace aparece em pelo menos
     * um failed path.
     */
    boolean appearsInFailedPath = false;

    for (Observation observation : observations) {

        if (observation.getResult() != ProbeResult.FAILED) {
            continue;
        }

        int[] failedPathLinks = observation.getFailedPathLinks();

        if (failedPathLinks == null) {
            continue;
        }

        for (int pathLinkId : failedPathLinks) {

            if (pathLinkId == linkId) {
                appearsInFailedPath = true;
                break;
            }
        }

        if (appearsInFailedPath) {
            break;
        }
    }

    /*
     * Sem evidencia produzida por failed probes,
     * permanece somente a probabilidade a priori.
     */
    if (!appearsInFailedPath) {
        return prior;
    }

    double t1 = calculateT1(
            linkId,
            linkStates,
            observations
    );

    double rv = calculateRv(
            linkId,
            linkStates,
            observations
    );

    double t2 = calculateT2(
            rv,
            prior,
            k
    );

    double failureCentrality =
            Math.max(t1, t2);

    /*
     * T1 = 1 representa identificacao deterministica
     * do componente falho.
     */
    if (t1 == 1.0) {
        linkStates.put(
                linkId,
                ComponentState.KNOWN_FAILED
        );
    }

    return failureCentrality;
}
    /**
     * Executa a inferencia deterministica baseada em T1
     * para todos os enlaces ainda conhecidos pelo algoritmo.
     */
    public Map<Integer, Double> calculateDeterministic(
            Map<Integer, ComponentState> linkStates,
            List<Observation> observations) {

        Map<Integer, Double> centrality = new HashMap<>();

        for (Map.Entry<Integer, ComponentState> entry
                : linkStates.entrySet()) {

            int linkId = entry.getKey();
            ComponentState state = entry.getValue();

            /*
             * Enlace comprovadamente operacional.
             */
            if (state == ComponentState.KNOWN_WORKING) {
                centrality.put(linkId, 0.0);
                continue;
            }

            /*
             * Enlace ja identificado como falho.
             */
            if (state == ComponentState.KNOWN_FAILED) {
                centrality.put(linkId, 1.0);
                continue;
            }

            /*
             * Para enlaces UNKNOWN calculamos T1.
             */
            double t1 = calculateT1(
                    linkId,
                    linkStates,
                    observations
            );

            centrality.put(linkId, t1);

            /*
             * T1 = 1 representa identificacao deterministica
             * da falha.
             */
            if (t1 == 1.0) {
                linkStates.put(
                        linkId,
                        ComponentState.KNOWN_FAILED
                );
            }
        }

        return centrality;
    }
}