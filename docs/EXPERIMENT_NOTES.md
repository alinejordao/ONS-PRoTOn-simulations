# Experiment Notes

## Branch
`feature/failure-centrality`

## Baseline
Tag preservada:
`proton-ons-testes-final-v1`

## Outputs locais
Preservados via stash:
`Outputs locais antes da Failure Centrality`

## Testes conceituais FC
```text
T1 = 1.0
Rv = 0.6666666666666666
T2 ≈ 0.46
FC determinística = 1.0
FC probabilística = 0.46
```
Todos passaram.

## Execução integrada
Já observados em desenvolvimento:
- 1842 links como `UNKNOWN`
- probe FAILED
- probe WORKING
- KSP
- rejeição por modulação
- criação de lightpath
- restauração
- expansão temporária do `knownGraph`

## XMLs
Existem 3 XMLs separados:
- D1
- D2
- D3

Cada um contém seu próprio `disaster-area`.

Prior será futuramente passado via:
```xml
<ra module="PRoTOn_ONS" prior="..." />
```

## Testes oficiais futuros
Fase 12:
```text
D1 / seed conhecida / load 50
D2 / mesma seed / load 50
D3 / mesma seed / load 50
```

## Pendências metodológicas
- recuperar percentuais/configurações candidatas testadas;
- registrar resultados que motivaram D1/D2/D3;
- documentar raios e enlaces;
- justificar cenários aninhados;
- formalizar associação final π ↔ D1/D2/D3.

## Pendências técnicas
- `scenarioPrior`
- `PRoTOn_ONS.setParameters()`
- validar prior em D1/D2/D3
- substituir `initialKnowledgeProbability`
- substituir `discoveryProbability`
- remover `pathUncertaintyProbability`
- integrar FC ao ciclo
- seleção de probes
- Monitor Placement

## Revisão futura
Verificar por que `delayedFlowArrival` usa `K=1` enquanto `restoreCriticalFlows` usa `K=10`.

## Regra de desenvolvimento
1. atualizar roadmap/notas;
2. teste pequeno;
3. confirmar comportamento;
4. commit;
5. avançar.
