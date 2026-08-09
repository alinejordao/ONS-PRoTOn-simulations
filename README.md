# 🌐 ONS-PRoTOn-simulations

> **Progressive Network Recovery adapted to the Optical Network
> Simulator (ONS)**\
> Pesquisa experimental sobre **recuperação progressiva pós-desastre em
> Redes Ópticas Elásticas (EON)**.

[![Status](https://img.shields.io/badge/status-baseline%20funcional-6f42c1)](#-status-do-projeto)
[![Version](https://img.shields.io/badge/tag-proton--ons--v1.0-7F00FF)](#-versionamento)
[![Research](https://img.shields.io/badge/contexto-pesquisa%20acad%C3%AAmica-0078FF)](#-contexto-da-pesquisa)

------------------------------------------------------------------------

## 🎯 Sobre o projeto

Este repositório contém a adaptação e implementação de mecanismos de
**recuperação progressiva de conexões após eventos de desastre** no
**ONS (Optical Network Simulator)**.

A implementação atual, denominada **`PRoTOn_ONS`**, adapta ao ambiente
de Redes Ópticas Elásticas do ONS conceitos de recuperação progressiva
inspirados no **PRoTOn / Net-MARS**.

O objetivo é construir uma base experimental rastreável e reproduzível
que permita, posteriormente, comparar diferentes estratégias de
recuperação sob as mesmas condições de simulação.

------------------------------------------------------------------------

## 🚦 Status do projeto

### ✅ PRoTOn_ONS v1.0

A primeira baseline funcional foi consolidada na tag:

``` text
proton-ons-v1.0
```

Essa versão representa um ponto estável da implementação e será
utilizada como referência para a próxima fase: **exercício experimental
controlado**.

### Principais características

-   🌩️ detecção de conexões afetadas por desastre;
-   🗺️ construção do estado físico pós-falha;
-   👁️ `knownGraph` inicialmente parcial;
-   🔎 descoberta progressiva de enlaces operacionais;
-   📋 classificação dos fluxos interrompidos;
-   🔁 múltiplas iterações de recuperação;
-   🧭 busca de rotas alternativas;
-   📡 seleção de modulação de acordo com a distância;
-   🧩 verificação e alocação de espectro;
-   💡 tentativa de estabelecimento de novos lightpaths;
-   ♻️ registro explícito de fluxos restaurados;
-   ⛔ descarte de fluxos quando a recuperação não é possível;
-   📊 cálculo de `Restore rate` e `Drop rate`.

> **Importante:** `proton-ons-v1.0` é uma **baseline funcional de
> implementação**. Ela não representa, por si só, uma conclusão
> experimental de ganho de desempenho.

------------------------------------------------------------------------

## 🔬 Contexto da pesquisa

A proposta experimental prevê a comparação entre três abordagens:

``` text
ONS original
     │
     ├── PRoTOn_ONS
     │
     └── PRoTOn_ONS_MCFP
```

### 🟣 ONS original

Baseline do simulador utilizada como referência, sem as modificações de
recuperação progressiva introduzidas neste trabalho.

### 🟪 PRoTOn_ONS

Adaptação do mecanismo de recuperação progressiva ao ONS.

Baseline funcional:

``` text
proton-ons-v1.0
```

### 🔵 PRoTOn_ONS_MCFP

Extensão futura do `PRoTOn_ONS` incorporando uma estratégia baseada em
**MCFP (Multi-Commodity Flow Problem)**.

Essa implementação **ainda não faz parte** da baseline
`proton-ons-v1.0`.

------------------------------------------------------------------------

## 🗺️ Topologia Minnesota

A principal topologia utilizada nesta etapa do desenvolvimento é a **Minnesota**, originalmente disponibilizada pelo **Net-MARS** em formato GML.

📌 **Topologia original utilizada como referência:**  
[MINNESOTA.gml — Net-MARS](https://github.com/matteoprata/Net-MARS/blob/public_master/data/graphs/MINNESOTA.gml)

O arquivo `MINNESOTA.gml` foi convertido para uma representação XML compatível com o ONS.

------------------------------------------------------------------------

## 📏 Recalibração das distâncias

A topologia original não fornece diretamente uma distância física em
quilômetros pronta para ser utilizada com o modelo de alcance das
modulações do ONS.

Por isso, as coordenadas presentes no `MINNESOTA.gml` foram utilizadas
para calcular as distâncias euclidianas relativas entre os nós.

Foram avaliadas diferentes escalas de conversão e seus efeitos sobre a
distribuição das rotas nas faixas de alcance das modulações.

A escala adotada foi:

``` text
α = 3,0
```

Para cada enlace:

``` text
weight = 3,0 × distância_euclidiana_original
```

O `weight` passa a representar a distância utilizada pelo simulador no
cálculo das rotas e na seleção da modulação.

Os dois sentidos correspondentes ao mesmo enlace físico recebem o mesmo
`weight`.

------------------------------------------------------------------------

## ⏱️ Delay de propagação

O atraso de propagação foi derivado da distância representada pelo
`weight`.

Foi utilizada a aproximação:

``` text
200 km/ms
```

Portanto:

``` text
delay(ms) = weight(km) / 200
```

Essa parametrização aproxima a velocidade de propagação da luz em fibra
óptica e mantém uma relação direta entre distância física e atraso.

------------------------------------------------------------------------

## 📁 Arquivos da Minnesota

### `test/MINNESOTA_ONS.xml`

Topologia Minnesota utilizada atualmente nos experimentos.

Contém:

-   `weight` recalculado com `α = 3,0`;
-   valores equivalentes nos dois sentidos do enlace físico;
-   `delay` derivado da distância.

### `test/MINNESOTA_ONS_legacy.xml`

Versão anterior da conversão da Minnesota, preservada para
**rastreabilidade metodológica**.

------------------------------------------------------------------------

# 📡 Modelo de modulação

A seleção de modulação segue os limites definidos no ONS em:

``` text
src/ons/Modulation.java
```

  Modulação     Alcance máximo
  ----------- ----------------
  BPSK                 8000 km
  QPSK                 4000 km
  8QAM                 2000 km
  16QAM                1000 km
  32QAM                 500 km
  64QAM                 250 km
  128QAM                125 km
  256QAM                 62 km

A modulação é escolhida de acordo com a distância total da rota
candidata.

> 🚫 Se nenhuma modulação suporta fisicamente a distância da rota, a
> rota é rejeitada. A implementação não força artificialmente BPSK ou
> outra modulação como fallback.

------------------------------------------------------------------------

# 🧠 Recuperação progressiva

Uma característica central do `PRoTOn_ONS` é a distinção entre dois
estados da rede:

### 🌐 `postFailureGraph`

Representa o **estado físico real da rede após o desastre**.

Contém os enlaces que permanecem efetivamente operacionais após a
ocorrência da falha.

### 👁️ `knownGraph`

Representa a **visão conhecida pelo mecanismo de recuperação**.

Essa visão pode ser inicialmente incompleta. O algoritmo começa com
conhecimento parcial da infraestrutura sobrevivente e descobre
progressivamente novos enlaces operacionais.

``` text
Rede física pós-desastre
        │
        ▼
 postFailureGraph
        │
        │ conhecimento parcial
        ▼
    knownGraph
        │
        │ descoberta progressiva
        ▼
 knownGraph expandido
        │
        ▼
 novas tentativas de restauração
```

Essa separação permite representar recuperação progressiva sem assumir
conhecimento completo e imediato da rede sobrevivente.

------------------------------------------------------------------------

# 🔄 Fluxo simplificado da recuperação

``` text
🌩️ Evento de desastre
        │
        ▼
🔗 Identificação dos enlaces afetados
        │
        ▼
⚠️ Identificação dos fluxos interrompidos
        │
        ▼
🌐 Construção do postFailureGraph
        │
        ▼
👁️ Construção do knownGraph parcial
        │
        ▼
📋 Classificação dos fluxos
        │
        ▼
🧭 Busca de rota candidata
        │
        ▼
📏 Cálculo da distância
        │
        ▼
📡 Seleção da modulação
        │
        ▼
🧩 Cálculo dos slots necessários
        │
        ▼
💡 Tentativa de criação do lightpath
        │
        ├── ✅ sucesso → fluxo restaurado
        │
        └── ❌ falha
              │
              ▼
        🔎 expansão do knownGraph
              │
              ▼
           🔁 nova iteração
              │
              ▼
       ♻️ restauração ou ⛔ descarte
```

------------------------------------------------------------------------

# 📊 Métricas de recuperação

Além das métricas originalmente produzidas pelo simulador, a
implementação acompanha explicitamente:

``` text
Dropped Flows
Restored Flows
Drop rate
Restore rate
```

Considerando:

``` text
totalInterruptedFlows = droppedFlows + restoredFlows
```

as taxas são calculadas como:

``` text
Drop rate    = droppedFlows / totalInterruptedFlows
Restore rate = restoredFlows / totalInterruptedFlows
```

Quando nenhum fluxo é interrompido:

``` text
Drop rate = 0
Restore rate = 0
```

evitando divisão por zero e resultados estatísticos inválidos.

------------------------------------------------------------------------

# 🗂️ Estrutura atual do repositório

``` text
ONS-PRoTOn-simulations/
│
├── 📂 src/
│   ├── ons/
│   │   ├── ControlPlane.java
│   │   ├── MyStatistics.java
│   │   ├── Modulation.java
│   │   └── ...
│   │
│   └── ra/
│       ├── PRoTOn_ONS.java
│       └── ...
│
├── 📂 test/
│   ├── MINNESOTA_ONS.xml
│   ├── MINNESOTA_ONS_legacy.xml
│   └── ...
│
├── 📂 Chart/
│   └── testes/
│       └── PRoTOn_ONS/
│
├── 📂 commons-io-2.15.1/
├── 📂 nbproject/
│
├── build.xml
├── manifest.mf
├── simulation-fdm_rmlsa_int.xml
├── saida
├── .gitignore
└── README.md
```

A estrutura poderá ser ampliada durante a fase experimental para separar
de forma explícita configurações, documentação e resultados
consolidados, evitando alterações prematuras que possam quebrar caminhos
utilizados pelo simulador.

------------------------------------------------------------------------

# ▶️ Execução

O projeto está sendo desenvolvido e executado utilizando **NetBeans
IDE**.

A classe principal recebe:

``` text
simulation_file seed numSeed minload maxload step
```

Exemplo utilizado durante a validação da Minnesota:

``` text
test\MINNESOTA_ONS.xml 1 1 50 50 1
```

  Parâmetro                Valor
  ------------------------ --------------------------
  Topologia/configuração   `test\MINNESOTA_ONS.xml`
  Seed inicial             `1`
  Número de seeds          `1`
  Carga mínima             `50`
  Carga máxima             `50`
  Step                     `1`

Os parâmetros experimentais serão definidos na próxima etapa, antes da execução dos testes.

------------------------------------------------------------------------

# 🧪 Validação funcional

Os testes executados durante o desenvolvimento da `v1.0` tiveram como
objetivo verificar o comportamento funcional da implementação,
incluindo:

-   🌩️ detecção dos fluxos interrompidos;
-   🧭 existência e descoberta de conectividade alternativa;
-   👁️ expansão progressiva do `knownGraph`;
-   📏 cálculo da distância das rotas;
-   📡 seleção da modulação;
-   🚫 rejeição de rotas fora do alcance físico;
-   🧩 disponibilidade espectral;
-   💡 criação de lightpaths;
-   ♻️ restauração de fluxos;
-   ⛔ descarte quando a recuperação não é possível;
-   📊 cálculo das métricas de recuperação.

Resultados quantitativos de desempenho serão produzidos em uma
**experimentos controlados**, utilizando parâmetros comuns às
estratégias comparadas.

------------------------------------------------------------------------

# ⚖️ Plano experimental

A próxima fase consiste na definição de uma matriz experimental comum.

As abordagens deverão utilizar, sempre que metodologicamente aplicável,
os mesmos:

-   🗺️ cenários/topologias;
-   📈 níveis de carga;
-   🎲 seeds;
-   🌩️ eventos de desastre;
-   📡 parâmetros ópticos;
-   📊 métricas;
-   🧪 critérios de avaliação.

Objetivo comparativo:

``` text
ONS original
      ×
PRoTOn_ONS
      ×
PRoTOn_ONS_MCFP
```

A primeira campanha será baseada na **Minnesota**.

Posteriormente, pretende-se avaliar as implementações em pelo menos uma
**topologia nativa do ONS**, permitindo observar seu comportamento fora
do cenário utilizado durante a adaptação inicial.

------------------------------------------------------------------------

# 🏷️ Versionamento

## `proton-ons-v1.0`

Baseline funcional do **PRoTOn_ONS**.

Inclui:

-   ✅ recuperação progressiva;
-   ✅ `knownGraph` parcial;
-   ✅ expansão progressiva do conhecimento da rede;
-   ✅ seleção de modulação baseada na distância;
-   ✅ restrições físicas de alcance;
-   ✅ restauração e descarte explícitos;
-   ✅ `Restore rate` e `Drop rate`;
-   ✅ proteção contra divisão por zero nas estatísticas;
-   ✅ Minnesota recalibrada com `α = 3,0`.

> 🔒 Essa tag representa o ponto de referência da implementação
> `PRoTOn_ONS` para os experimentos posteriores.

------------------------------------------------------------------------

# 🚀 Roadmap

### Fundação

-   [x] Conversão da topologia Minnesota para ONS
-   [x] Análise das distâncias da Minnesota
-   [x] Definição de `α = 3,0`
-   [x] Recalibração de `weight`
-   [x] Recalibração de `delay`

### PRoTOn_ONS

-   [x] Implementação da recuperação progressiva
-   [x] `knownGraph` parcial
-   [x] Expansão progressiva do conhecimento
-   [x] Restrições de alcance das modulações
-   [x] Validação funcional da restauração
-   [x] Baseline `proton-ons-v1.0`

### Experimentos

-   [ ] Definição da matriz experimental
-   [ ] Execução sistemática do `PRoTOn_ONS`
-   [ ] Preparação da baseline do ONS original
-   [ ] Consolidação dos resultados

### Próxima evolução

-   [ ] Implementação do `PRoTOn_ONS_MCFP`
-   [ ] Execução da mesma matriz nas três abordagens
-   [ ] Comparação quantitativa
-   [ ] Análise estatística
-   [ ] Validação em topologia adicional do ONS

------------------------------------------------------------------------

# 📚 Referências de implementação

O desenvolvimento utiliza como referências de software e dados:

- **ONS — Optical Network Simulator**
- **Net-MARS / PRoTOn**
- 🗺️ [MINNESOTA.gml — topologia original utilizada](https://github.com/matteoprata/Net-MARS/blob/public_master/data/graphs/MINNESOTA.gml)

As referências bibliográficas formais utilizadas na dissertação serão mantidas na documentação acadêmica correspondente.

------------------------------------------------------------------------

# 🎓 Objetivo acadêmico

Mais do que disponibilizar código, este repositório busca preservar a
**rastreabilidade das decisões experimentais**.

Transformações metodologicamente relevantes --- como conversão da
topologia, calibração das distâncias, parametrização de `delay`,
restrições de modulação, estratégias de recuperação e configurações
experimentais --- devem permanecer documentadas para permitir:

> 🔬 **reprodução → comparação → análise → defesa metodológica**

------------------------------------------------------------------------

### 💜 Pesquisa em andamento

**Recuperação Progressiva Conjunta em Redes Ópticas Elásticas
Pós-Desastre**

`ONS` · `EON` · `PRoTOn` · `Net-MARS` · `Progressive Recovery` · `MCFP`
