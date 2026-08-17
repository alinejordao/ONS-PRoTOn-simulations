package ra.monitoring;

/**
 * Resultado de uma sondagem realizada entre dois monitores.
 *
 * WORKING indica que o caminho percorrido pelo probe está operacional.
 * FAILED indica que o probe não conseguiu atravessar o caminho completo.
 *
 * Um resultado FAILED não implica que todos os enlaces do caminho
 * estejam falhos. A identificação dos componentes suspeitos será
 * realizada posteriormente pela lógica de Failure Centrality.
 */
public enum ProbeResult {

    WORKING,
    FAILED
}