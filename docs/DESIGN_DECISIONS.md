# Design Decisions

## 1. Não reimplementar Net-MARS em Java
O PRoTOn_ONS adapta conceitos do PRoTOn ao ONS/EON.

## 2. Ground truth × conhecimento
Ground truth:
- `DisasterArea`
- `isIsInterupted`
- `postFailureGraph`

Conhecimento:
- `ComponentState`
- `Probe`
- `Observation`
- Failure Centrality
- `knownGraph`

## 3. Não usar `area.getLinks()` como conhecimento
Isso revelaria diretamente as falhas e eliminaria a necessidade de probes/FC.

## 4. π não vem do ground truth em runtime
π é parâmetro experimental.

## 5. `RA.setParameters(...)`
Criado para permitir parâmetros de algoritmo sem acoplar `ControlPlane` à Failure Centrality.

```text
XML → Simulator → raParameters → ControlPlane → RA.setParameters(...) → algoritmo
```

## 6. Adaptação de Rv
Validação conceitual:
```text
P1=[e1,e2,e3]
P2=[e2,e4]
e1=KNOWN_WORKING
→ P1=[e2,e3]
→ P2=[e2,e4]
→ união={e2,e3,e4}
→ Rv(e2)=2/3
```

## 7. FC ≠ estado categórico
- FC intermediária = suspeita
- `T1=1` = certeza determinística
- somente então → `KNOWN_FAILED`

## 8. Preservar EON do ONS
Manter:
- modulação
- slots
- lightpaths
- `acceptFlow`
- `restoreFlow`
- `dropFlow`

## 9. `buildPostFailureGraph()`
**MANTER**

## 10. `buildInitialKnownGraph()`
Manter conceito; substituir `initialKnowledgeProbability`.

## 11. `expandKnownGraph()`
Substituir `discoveryProbability`.

## 12. `pathUncertaintyProbability=0.2`
Temporário; substituir por monitoramento/FC.

## 13. KSP
- recuperação: `K=10`
- fluxos atrasados: `K=1`

### Revisão futura
Verificar por que fluxos atrasados usam `K=1` enquanto a recuperação progressiva usa múltiplos caminhos.

## 14. `computeScore()`
Manter por enquanto. Não alterar simultaneamente score, FC e RSA.
