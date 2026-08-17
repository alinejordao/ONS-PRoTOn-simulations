# PRoTOn_ONS Architecture

## Objetivo
Adaptar a lógica do PRoTOn ao ONS/EON sem reimplementar o Net-MARS em Java.

Preservar do ONS: KSP, modulação, slots, lightpaths, `acceptFlow`, `restoreFlow`, `dropFlow` e o mecanismo físico de desastre.

## Net-MARS/PRoTOn × ONS/EON
- Net-MARS/PRoTOn: fonte conceitual para monitoramento, probes, observações, inferência e Failure Centrality.
- ONS/EON: ambiente de simulação e restrições ópticas.
- PRoTOn_ONS: adaptação entre os dois.

## Ground truth × conhecimento do protocolo
### `postFailureGraph`
Representa o estado físico real pós-falha. É ground truth interno do simulador.

### `knownGraph`
Representa somente o conhecimento atual do protocolo. No estado final deve evoluir por:
`Probe → Observation → Failure Centrality → ComponentState → knownGraph`.

## Estados
- `UNKNOWN`
- `KNOWN_WORKING`
- `KNOWN_FAILED`

## Fluxo desejado
```text
TOPOLOGIA ORIGINAL
       ↓
DisasterArea / ONS
       ↓
postFailureGraph (ground truth)
       ↓
Probe
       ↓
Observation
       ↓
Failure Centrality
       ↓
ComponentState
       ↓
knownGraph
       ↓
KSP
       ↓
modulação + slots
       ↓
lightpath
       ↓
restoreFlow / dropFlow
       ↓
nova iteração
```

## Probe
Registra origem, destino, `pathNodes` (`int[]`), `pathLinks` (`int[]`), iteração e resultado `WORKING`/`FAILED`.

## Observation
- WORKING: todos os enlaces do caminho podem virar `KNOWN_WORKING`.
- FAILED: registra os enlaces ainda desconhecidos do caminho como failed probe path, sem marcar todos como falhos.

## Failure Centrality
Implementação atual:
- `T1`
- `Rv`
- `T2`
- `FC = max(T1,T2)`

Regras:
- `KNOWN_WORKING` → `FC=0`
- `KNOWN_FAILED` → `FC=1`
- `T1=1` → falha determinística → `KNOWN_FAILED`
- FC intermediária → componente continua `UNKNOWN`

## Prior π
Não deve ser obtido de `DisasterArea.getLinks()` em runtime.

Fluxo de configuração:
```text
XML → Simulator → raParameters → ControlPlane → RA.setParameters(...) → PRoTOn_ONS → scenarioPrior
```

## Elementos temporários a substituir
- `initialKnowledgeProbability`
- `discoveryProbability`
- `pathUncertaintyProbability`

## Elementos do ONS a preservar
- `buildPostFailureGraph()`
- KSP
- modulação
- slots
- lightpaths
- `acceptFlow`
- `restoreFlow`
- `dropFlow`
