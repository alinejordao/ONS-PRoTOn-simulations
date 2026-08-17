
package ra.monitoring;

/**
 * Representa uma sondagem (probe) realizada entre dois monitores.
 *
 * O Probe registra:
 *  - monitor de origem;
 *  - monitor de destino;
 *  - caminho percorrido em termos de nos;
 *  - enlaces correspondentes ao caminho;
 *  - iteracao em que foi executado;
 *  - resultado observado.
 *
 * O Probe nao realiza restauracao de trafego e nao reserva recursos
 * opticos. Sua funcao e exclusivamente representar uma sondagem da rede.
 */
public class Probe {

    private final int sourceMonitor;
    private final int destinationMonitor;

    private final int[] pathNodes;
    private final int[] pathLinks;

    private final int iteration;

    private ProbeResult result;

    public Probe(
            int sourceMonitor,
            int destinationMonitor,
            int[] pathNodes,
            int[] pathLinks,
            int iteration) {

        this.sourceMonitor = sourceMonitor;
        this.destinationMonitor = destinationMonitor;
        this.pathNodes = pathNodes;
        this.pathLinks = pathLinks;
        this.iteration = iteration;

        // O resultado somente sera conhecido apos a execucao do probe.
        this.result = null;
    }

    public int getSourceMonitor() {
        return sourceMonitor;
    }

    public int getDestinationMonitor() {
        return destinationMonitor;
    }

    public int[] getPathNodes() {
        return pathNodes;
    }

    public int[] getPathLinks() {
        return pathLinks;
    }

    public int getIteration() {
        return iteration;
    }

    public ProbeResult getResult() {
        return result;
    }

    public void setResult(ProbeResult result) {
        this.result = result;
    }
}