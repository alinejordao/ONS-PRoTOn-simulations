package ra.monitoring;

/**
 * Representa uma observacao obtida a partir da execucao de um Probe.
 *
 * Uma observacao WORKING confirma como operacionais os enlaces
 * percorridos pelo probe.
 *
 * Uma observacao FAILED registra os enlaces pertencentes ao caminho
 * que falhou. Esses enlaces sao candidatos/suspeitos e NAO devem ser
 * considerados individualmente como falhos neste momento.
 *
 * A identificacao da relevancia de cada componente sera realizada
 * posteriormente pela Failure Centrality.
 */
public class Observation {

    private final Probe probe;
    private final ProbeResult result;

    private final int[] workingLinksConfirmed;
    private final int[] failedPathLinks;

    private final int iteration;

    public Observation(
            Probe probe,
            ProbeResult result,
            int[] workingLinksConfirmed,
            int[] failedPathLinks,
            int iteration) {

        this.probe = probe;
        this.result = result;
        this.workingLinksConfirmed = workingLinksConfirmed;
        this.failedPathLinks = failedPathLinks;
        this.iteration = iteration;
    }

    public Probe getProbe() {
        return probe;
    }

    public ProbeResult getResult() {
        return result;
    }

    public int[] getWorkingLinksConfirmed() {
        return workingLinksConfirmed;
    }

    public int[] getFailedPathLinks() {
        return failedPathLinks;
    }

    public int getIteration() {
        return iteration;
    }
}