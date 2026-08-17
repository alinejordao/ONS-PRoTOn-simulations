# Failure Centrality Roadmap

Este é o **roadmap mestre/congelado**.

## Fase 0 — Garantir que a versão atual está salva
- [x] `git status`
- [x] `git log --oneline --decorate -5`
- [x] `main` e `origin/main` sincronizados
- [x] tag `proton-ons-testes-final-v1` preservada
- [x] alterações locais avaliadas
- [x] outputs locais preservados via stash

## Fase 1 — Criar linha Failure Centrality
- [x] partir de `main`
- [x] atualizar `main`
- [x] criar `feature/failure-centrality`
- [x] confirmar commit de origem
- [x] preservar a tag
- [x] publicar `origin/feature/failure-centrality`

## Fase 2 — Mapear o PRoTOn_ONS
- [x] `postFailureGraph`
- [x] `knownGraph`
- [x] descoberta probabilística
- [x] fluxos interrompidos
- [x] expansão do `knownGraph`
- [x] KSP
- [x] avaliação de caminhos
- [x] modulação
- [x] slots
- [x] lightpath
- [x] `restoreFlow`
- [x] `dropFlow`

## Fase 3 — Modelo de monitoramento
- [ ] definir formalmente `Monitor`
- [ ] definir formalmente quais nós podem ser monitores
- [x] definir probe
- [x] `WORKING` / `FAILED`
- [x] conhecimento após probe bem-sucedido
- [x] significado de probe falho
- [x] separar estado real × estado conhecido

**Status:** 🟡 conceitualmente suficiente; os dois primeiros itens serão detalhados na Fase 9.

## Fase 4 — Probe e Observation
- [x] origem
- [x] destino
- [x] caminho
- [x] componentes
- [x] resultado
- [x] iteração
- [x] `Probe`
- [x] `ProbeResult`
- [x] `Observation`
- [x] `executeProbe()`
- [x] `createObservation()`
- [x] validar WORKING
- [x] validar FAILED

## Fase 5 — Failure Centrality
- [x] failed probe paths
- [x] componentes confirmadamente operacionais
- [x] `T1`
- [x] `T2`
- [x] `Rv`
- [x] `FC=max(T1,T2)`
- [x] `KNOWN_WORKING → FC=0`
- [x] falha determinística com `T1=1`
- [x] FC intermediária não promove automaticamente para `KNOWN_FAILED`
- [x] revisar prior no Net-MARS
- [x] revisar desastre no ONS
- [x] ground truth × conhecimento
- [x] não usar `area.getLinks()` como conhecimento
- [x] π como parâmetro experimental
- [x] `RA.setParameters(...)`
- [x] `Simulator` coleta parâmetros
- [x] `ControlPlane` entrega parâmetros ao RA
- [x] projeto compilando/executando após a infraestrutura
- [ ] criar `scenarioPrior` no `PRoTOn_ONS`
- [ ] implementar `PRoTOn_ONS.setParameters()`
- [ ] receber `prior` do XML
- [ ] validar D1
- [ ] validar D2
- [ ] validar D3

**Status:** 🟡 ESTAMOS TERMINANDO ESTA FASE.  
**Próximo passo exato:** criar `scenarioPrior` no `PRoTOn_ONS` e implementar `setParameters()`.

## Fase 6 — Teste unitário/conceitual
- [x] cenário artificial
- [x] múltiplos caminhos
- [x] caminho/componente funcionando
- [x] caminho falhando
- [x] validar T1 manualmente
- [x] validar Rv manualmente
- [x] validar T2 manualmente
- [x] validar FC completa
- [x] comparar com Java
- [x] validar `KNOWN_FAILED` apenas no caso determinístico
- [x] validar `UNKNOWN` para FC intermediária

Referência:
```text
T1 = 1.0 → PASS
Rv = 2/3 → PASS
T2 = 0.46 → PASS
FC determinística = 1.0 → PASS
FC probabilística = 0.46 → PASS
```

## Fase 7 — Substituir descoberta probabilística fixa
- [ ] remover `initialKnowledgeProbability`
- [x] manter `postFailureGraph` como verdade física
- [ ] remover `discoveryProbability`
- [ ] fazer `knownGraph` refletir somente conhecimento inferido
- [ ] remover/substituir `pathUncertaintyProbability`

**Status:** ⬜ próxima grande fase.

## Fase 8 — Seleção dos probes
- [ ] heurística determinística
- [ ] evitar redundância
- [ ] priorizar caminhos informativos
- [ ] aproximar FaCeGreedy, se necessário

## Fase 9 — Monitor Placement
- [ ] definir formalmente Monitor no ONS
- [ ] definir nós candidatos
- [ ] orçamento
- [ ] quantidade por iteração
- [ ] utilidade
- [ ] adicionar monitores

## Fase 10 — Integrar com ciclo PRoTOn_ONS
`desastre → monitoramento → observações → FC → knownGraph → KSP → modulação → slots → lightpath → restauração → nova iteração`

## Fase 11 — Logs
- [ ] monitores ativos
- [x] probes enviados — preliminar
- [x] resultado dos probes — preliminar
- [x] componentes descobertos — preliminar
- [ ] FC por componente
- [x] mudanças no `knownGraph` — preliminar
- [x] caminhos KSP
- [x] rejeição de rotas
- [x] fluxos restaurados
- [x] fluxos pendentes

## Fase 12 — Testes funcionais pequenos
- [ ] D1 / seed conhecida / load 50
- [ ] D2 / mesma seed / load 50
- [ ] D3 / mesma seed / load 50

## Fase 13 — Comparação PRoTOn_ONS × PRoTOn_ONS-FC
Mesmos XMLs, seeds, loads e restrições ópticas.

Comparar:
- Restore rate
- Drop rate
- número de iterações
- conhecimento adquirido
- número de probes
- tempo de recuperação
- custo computacional

## Fase 14 — Congelar Failure Centrality
- [ ] README
- [ ] documentação dos testes
- [ ] adaptar `.bat`
- [ ] tag própria
- [ ] preservar baseline

## Fase 15 — Fluxo mínimo / MCFP
- [ ] formulação matemática
- [ ] separar contribuição do PRoTOn original
- [ ] implementar
- [ ] comparar

## Status oficial
```text
Fase 0  ✅
Fase 1  ✅
Fase 2  ✅
Fase 3  🟡 conceitualmente suficiente
Fase 4  ✅
Fase 5  🟡 ← ESTAMOS TERMINANDO ESTA
Fase 6  ✅
Fase 7  ⬜ ← PRÓXIMA GRANDE FASE
Fase 8  ⬜
Fase 9  ⬜
Fase 10 ⬜
Fase 11 🟡 logs preliminares
Fase 12 ⬜
Fase 13 ⬜
Fase 14 ⬜
Fase 15 ⬜
```

## Matriz de decisões
| Bloco | Decisão |
|---|---|
| `disasterArrival()` | **ADAPTAR** |
| `buildPostFailureGraph()` | **MANTER** |
| `buildInitialKnownGraph()` | **MANTER conceito / SUBSTITUIR lógica aleatória** |
| `expandKnownGraph()` | **SUBSTITUIR** |
| `computeScore()` | **MANTER por enquanto** |
| `restoreCriticalFlows()` | **ADAPTAR** |
| KSP `K=10` | **MANTER** |
| `tryRecovery()` | **MANTER núcleo EON / REMOVER incerteza aleatória depois** |
| modulação / slots / lightpath | **MANTER** |
| `restoreFlow()` / `dropFlow()` | **MANTER** |
| `pathUncertaintyProbability = 0.2` | **SUBSTITUIR futuramente por monitoramento/FC** |
