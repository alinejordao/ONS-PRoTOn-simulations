# Simulações em Redes Ópticas Elásticas

arrigoni.java - A progressive, uncertainty-aware recovery algorithm with partial network knowledge reconstruction for Elastic Optical Networks

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
