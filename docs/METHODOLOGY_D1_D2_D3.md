# Methodology — D1 / D2 / D3

## Importância
A construção de D1/D2/D3 é parte metodológica da dissertação.

Eles não foram escolhidos arbitrariamente. Antes da configuração final, foram testadas várias combinações de percentuais/severidades para encontrar níveis que:
- fossem distinguíveis;
- não fossem fracos demais;
- não inviabilizassem a comparação;
- permitissem comparação controlada entre algoritmos.

## Princípio
Topologia Minnesota, mesmo centro/epicentro experimental e raios crescentes.

```text
Minnesota
   ↓
mesmo centro
   ↓
raios crescentes
   ↓
D1 ⊂ D2 ⊂ D3
```

## Configuração final registrada
| Cenário | Raio | Enlaces físicos | Percentual aprox. |
|---|---:|---:|---:|
| D1 | 180 | 19 | 2,06% |
| D2 | 274 | 46 | 4,99% |
| D3 | 360 | 93 | 10,10% |

Representação direcionada nos XMLs:
- D1 → 38 links
- D2 → 92 links
- D3 → 186 links

Total de referência: 921 enlaces físicos.

## Processo de escolha
**Reconstruir a partir do histórico original antes de escrever a dissertação:**
1. percentuais/configurações candidatas;
2. resultados preliminares;
3. critérios de descarte;
4. razão da escolha final;
5. por que a separação final foi considerada adequada.

Não preencher lacunas por suposição.

## Justificativa
- reprodutibilidade;
- severidade crescente;
- comparação justa;
- controle espacial;
- mesmos cenários para todos os algoritmos;
- separação entre falha e recuperação.

## Relação com π
π será parâmetro experimental, não leitura do ground truth em runtime.

Forma candidata:
```text
π_D = enlaces físicos afetados na definição do cenário / total de enlaces físicos
```

- D1 → 19/921
- D2 → 46/921
- D3 → 93/921

A associação final deve ser explicada junto da reconstrução dos testes de percentuais.
