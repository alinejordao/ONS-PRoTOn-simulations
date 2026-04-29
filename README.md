# Simulações em Redes Ópticas Elásticas

PRoTOn-ONS, Adaptive Progressive Recovery 

A progressive, uncertainty-aware recovery algorithm with partial network knowledge reconstruction for Elastic Optical Networks

Este repositório contém adaptações e experimentos realizados no simulador ONS para avaliação de cenários em Redes Ópticas Elásticas, com foco em recuperação progressiva de serviços após falhas de larga escala.

## Estrutura do Projeto

- `src/`: código-fonte do simulador ONS.
- `lib/`: bibliotecas necessárias para execução.
- `topologies/original/`: topologias originais obtidas de fontes externas.
- `topologies/converted/`: topologias convertidas para o formato XML compatível com o ONS.
- `simulations/`: arquivos XML de configuração das simulações.
- `results/`: saídas e resultados das execuções.
- `docs/`: anotações, referências e documentação auxiliar.
- `scripts/`: scripts auxiliares para conversão, execução ou análise.

## Topologia Minnesota

A topologia Minnesota foi convertida para o formato XML do Net-MARS para utilização no ONS a partir de uma topologia em formato GML.

A conversão inclui:

- nós da rede;
- enlaces bidirecionais;
- recalibração de `weight` como distância/custo;
- recalibração de `delay` proporcional ao peso do enlace;
- bloco `traffic`;
- bloco `QoS`.

## Como executar

Exemplo de execução pelo NetBeans:

1. Abrir o projeto no NetBeans IDE.
2. Selecionar a classe principal `ons.Main`.
3. Informar o arquivo XML de simulação desejado.
4. Executar o projeto.

## Tecnologias

- Java
- NetBeans IDE
- ONS - Optical Network Simulator
- XML
- GML

## Status

Projeto em desenvolvimento para fins acadêmicos e experimentais.

## Comparativo: PRoTOn original vs. implementação PRoTOn-like no ONS

| Aspecto | PRoTOn original | Implementação no ONS |
|---|---|---|
| Modelo de rede | Grafo abstrato com falhas e recuperação progressiva | Topologia física do ONS com enlaces, slots, modulação e lightpaths |
| Objetivo principal | Recuperar serviços críticos após falhas massivas usando tomografia de rede | Restaurar fluxos interrompidos em EONs usando recuperação progressiva adaptada ao ONS |
| Tomografia | Bayesian Network Tomography | Simulação de incerteza via probabilidade de falha na tentativa de rota |
| Conhecimento da rede | Probabilístico, atualizado por inferência | `knownGraph` atualizado parcialmente após restaurações bem-sucedidas |
| Recuperação | Progressiva, baseada em caminhos e reparos | Iterativa, com múltiplas rodadas de tentativa de restauração |
| Priorização | Baseada em impacto/custo da recuperação | Score com banda, custo da rota, número de hops e disponibilidade de slots |
| Alocação de recursos | Mais abstrata, focada no grafo | Específica do ONS: slots, modulação, lightpaths e `acceptFlow` |
| Fluxos interrompidos | Serviços críticos são priorizados | Todos os fluxos interrompidos são tratados como críticos nesta versão |
| Incerteza | Modelada matematicamente por tomografia bayesiana | Aproximada por rejeição probabilística de rotas viáveis |
| Aprendizado da rede | Atualização probabilística do estado dos elementos | Aprendizado parcial dos links usados em rotas restauradas |
| Busca por solução | Estratégia orientada por custo/benefício | KSP + limite de tentativas por slots |
| Diferença principal | Algoritmo original com inferência probabilística formal | Adaptação prática ao ONS, respeitando restrições físicas de EON |
| Contribuição da adaptação | — | Integra recuperação progressiva com espectro, modulação e provisionamento realista |
