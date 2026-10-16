# Motor de Recomendação

API REST de recomendação de produtos desenvolvida para o desafio Geração Caldeira. O motor combina dados de clientes, catálogo, histórico de compras, promoções e contexto externo (clima e feriados) para ordenar produtos ativos por uma pontuação explicável.


## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC e Spring Data JPA
- PostgreSQL
- Bean Validation
- Lombok
- Springdoc OpenAPI / Swagger UI
- Maven Wrapper

## Executar localmente

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, a API fica disponível em `http://localhost:8080`. O Swagger UI fica em `http://localhost:8080/swagger-ui.html`.

## Estrutura da aplicação

```text
src/main/java/com/desafiogc/RecommedationEngine/
├── api/                 # Controllers REST e DTOs de entrada/saída
├── config/              # Configuração de clientes HTTP
├── integration/         # Clientes Open-Meteo e BrasilAPI
├── model/               # Entidades e modelos de domínio
├── repository/          # Repositórios Spring Data JPA
└── service/             # Ingestão, contexto, calendário e recomendação
```

Fluxo principal: Controller → Service → Repository/JPA → PostgreSQL. Para recomendações, o serviço também consulta Open-Meteo e BrasilAPI.

## Banco de dados

O projeto espera as tabelas já criadas no schema `public`. O mapeamento atual usa estes nomes:

| Tabela | Colunas usadas pela aplicação |
|---|---|
| `cliente` | `id_cliente`, `cnpj`, `nome`, `segmento`, `subsegmento`, `numero_celular`, `cep`, `ultima_compra`, `id_hierarquia`, `id_cidade` |
| `cidade` | `id_cidade`, `nome`, `uf` |
| `hierarquia` | `id_hierarquia` (referência armazenada no cliente) |
| `produto` | `id_item`, `nome`, `cod_familia`, `preco_base`, `preco_minimo`, `ativo` |
| `produto_complementar` | `produto_id`, `complementar_id` |
| `pedido` | `id_pedido`, `data_pedido`, `id_cliente` |
| `item_pedido` | `pedido_id`, `produto_id`, `quantidade`, `valor_unitario` |
| `promocao` | `id_promocao`, `nome`, `produto_id`, `categoria`, `tipo_desconto`, `valor_desconto`, `inicio`, `fim`, `ativa` |
| `notificacao` | `id_notificacao`, `cliente_id`, `tipo`, `mensagem`, `canal`, `status`, `criada_em` |

A propriedade `categoria` do modelo de produto corresponde à coluna `cod_familia`. A cidade do cliente é obtida pela relação `cliente.id_cidade` → `cidade.id_cidade`; a API de recomendação usa o nome da cidade para consultar o clima.

Os itens do pedido são mapeados com chave composta `(pedido_id, produto_id)`, pois a tabela informada não possui uma coluna de identificador própria. Isso pressupõe no máximo uma linha por produto em cada pedido.

As tabelas `estoque_erp` e outros campos do banco que não aparecem na lista de mapeamentos ainda não participam da lógica atual do recomendador.

### Versionamento com Flyway

As migrations SQL versionadas ficam em `src/main/resources/db/migration` e seguem o padrão `V<versão>__<descrição>.sql`. A migration `V1__criar_esquema_inicial.sql` cria as tabelas acima quando a aplicação inicia contra um schema `public` vazio.

No primeiro início contra o banco Supabase já existente, o Flyway cria `flyway_schema_history` e registra o schema atual como baseline na versão `1`. Esse baseline não reaplica a V1 e não recria nem apaga as tabelas existentes. O Hibernate valida o mapeamento com `spring.jpa.hibernate.ddl-auto=validate`. Depois desse baseline, novas alterações devem ser adicionadas em migrations novas, como `V2__descricao_da_alteracao.sql`; migrations já aplicadas não devem ser editadas.

O usuário PostgreSQL configurado para a aplicação precisa ter permissão para criar a tabela `flyway_schema_history` no schema `public` e executar as migrations futuras. O Flyway usa o mesmo datasource configurado para o Spring.

## Endpoints

Todos os exemplos usam `Content-Type: application/json` nos métodos `POST`.

### Clientes

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/clientes` | Lista até 50 clientes. |
| `POST` | `/api/clientes` | Cadastra cliente. A cidade pode ser informada pelo ID ou pelo nome de uma cidade já existente. |

Exemplo de cadastro:

```http
POST http://localhost:8080/api/clientes
Content-Type: application/json
```

```json
{
  "cnpj": "CNPJ_DE_TESTE",
  "nome": "Cliente Exemplo",
  "perfil": "Varejo",
  "subsegmento": "Mercado",
  "cep": "00000000",
  "hierarquiaId": 1,
  "cidadeId": 1,
  "telefone": "TELEFONE_DE_TESTE",
  "ultimaCompra": "2026-10-14"
}
```

`perfil` é persistido na coluna `segmento`. Para selecionar cidade pelo nome, substitua `cidadeId` por, por exemplo, `"cidade": "Cidade Exemplo"`. Use um ID ou nome cadastrado na tabela `cidade`.

A consulta `GET /api/clientes` retorna até 50 resultados com identificador, nome, segmento (`perfil`), cidade e data da última compra.

Exemplo de resposta:

```json
[
  {
    "id": 1,
    "nome": "Cliente Exemplo",
    "perfil": "Varejo",
    "cidade": "Cidade Exemplo",
    "ultimaCompra": "2026-10-14"
  }
]
```

### Produtos

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/produtos` | Cadastra produto e relações opcionais de complementaridade. |

Exemplo:

```http
POST http://localhost:8080/api/produtos
Content-Type: application/json
```

```json
{
  "nome": "Produto Exemplo",
  "categoria": "FAMILIA_DE_TESTE",
  "produtosComplementaresIds": [],
  "precoBase": 15.90,
  "precoMinimo": 12.00
}
```

A categoria é armazenada em `produto.cod_familia`. Os IDs em `produtosComplementaresIds` devem corresponder a produtos já cadastrados.

### Pedidos

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/pedidos` | Registra um pedido e seus itens. |

Exemplo:

```http
POST http://localhost:8080/api/pedidos
Content-Type: application/json
```

```json
{
  "clienteId": 1,
  "dataCompra": "2026-10-14",
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 2,
      "valorUnitario": 15.90
    }
  ]
}
```

O cliente e os produtos informados precisam existir. Quantidades devem ser positivas. A data enviada como `yyyy-MM-dd` é persistida como timestamp no início daquele dia.

### Promoções

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/promocoes` | Cadastra uma promoção por produto ou por categoria/família. |

Exemplo por produto:

```http
POST http://localhost:8080/api/promocoes
Content-Type: application/json
```

```json
{
  "nome": "Oferta de teste",
  "produtoId": 1,
  "tipoDesconto": "PERCENTUAL",
  "valorDesconto": 10.00,
  "inicio": "2026-10-14",
  "fim": "2026-10-31"
}
```

Tipos aceitos: `PERCENTUAL` e `VALOR_FIXO`. Para uma promoção por categoria, omita `produtoId` e envie `"categoria": "FAMILIA_DE_TESTE"`. Envie apenas um dos dois campos. O fim do período é considerado inclusivo.

### Recomendações

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/recomendacoes/{clienteId}` | Recomenda produtos para o cliente; usa a cidade cadastrada quando disponível. |
| `GET` | `/api/recomendacoes/{clienteId}?cidade={cidade}` | Permite informar uma cidade para esta consulta. |

Exemplo:

```http
GET http://localhost:8080/api/recomendacoes/1
```

Cada item da resposta informa `produtoId`, `nomeProduto`, `categoria`, `score`, `motivos` e `promocaoId`. O cliente precisa existir. Se clima ou calendário estiverem indisponíveis, a recomendação ainda pode ser calculada com os dados internos, e a resposta informa o fator contextual indisponível nos motivos.

Exemplo de resposta:

```json
[
  {
    "produtoId": 1,
    "nomeProduto": "Produto Exemplo",
    "categoria": "FAMILIA_DE_TESTE",
    "score": 30.0,
    "motivos": [
      "Categoria FAMILIA_DE_TESTE presente no histórico (1 item(ns), +5)",
      "Promoção vigente: Oferta de teste (+15)"
    ],
    "promocaoId": 1
  }
]
```

### Contexto: localização e clima

| Método | Endpoint | Fonte |
|---|---|---|
| `GET` | `/api/contexto/localizacao?cidade={cidade}` | Open-Meteo Geocoding |
| `GET` | `/api/contexto/clima?cidade={cidade}` | Open-Meteo Geocoding e Forecast |

Exemplos:

```http
GET http://localhost:8080/api/contexto/localizacao?cidade=São%20Paulo
GET http://localhost:8080/api/contexto/clima?cidade=São%20Paulo
```

A resposta de clima contém localização, horário da observação, temperatura, sensação térmica, umidade, precipitação, código WMO e vento.

### Contexto: feriados

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/contexto/feriados?ano={ano}` | Lista feriados nacionais. |
| `GET` | `/api/contexto/feriados?ano={ano}&uf={UF}` | Consulta feriados com UF opcional. |

Exemplos:

```http
GET http://localhost:8080/api/contexto/feriados?ano=2026
GET http://localhost:8080/api/contexto/feriados?ano=2026&uf=SP
```

A consulta usa a BrasilAPI. O ano aceito pela API vai de 1900 a 2199, e a UF deve conter duas letras.

## Como o score funciona (V1)

O motor gera pontuações explicáveis, sem modelo de machine learning treinado. Produtos inativos são ignorados. Cada produto ativo começa com 10 pontos:

| Fator | Pontos | Regra |
|---|---:|---|
| Histórico na mesma categoria/família | +5 por linha de item, até +25 | Conta itens de pedidos anteriores do cliente cuja categoria corresponde a `cod_familia`. |
| Produto complementar | +20 | O produto recomendado está relacionado a um produto comprado na tabela `produto_complementar`. |
| Promoção vigente | +15 | Aplica uma promoção correspondente ao produto ou à categoria. Se houver ambas, a promoção específica do produto tem prioridade. |
| Clima quente ou frio | +8 | Calor a partir de 27 °C ou frio até 18 °C, se a família do produto corresponder às categorias configuradas. |
| Chuva | +8 | Precipitação a partir de 0,5 mm ou código WMO de chuva, se a família corresponder às categorias configuradas. |
| Feriado próximo | +7 | Feriado nacional hoje ou nos próximos 7 dias e família compatível. |

Os limites, pesos e categorias de contexto podem ser ajustados em `application.properties` ou por variáveis de ambiente com prefixo `REC_`. A comparação das categorias ignora maiúsculas, minúsculas e acentos. Os produtos são ordenados por score decrescente; empates são ordenados pelo ID.

## Integrações externas

- **Open-Meteo:** busca a localização pelo nome da cidade e consulta o clima atual por coordenadas.
- **BrasilAPI:** consulta feriados por ano e, opcionalmente, UF.

As integrações precisam de acesso à internet. Falhas nas APIs externas não impedem a recomendação: o serviço registra um aviso e calcula o score sem o fator contextual indisponível.

## Limitações e próximos passos

- O schema é administrado fora do Hibernate; alterações no banco precisam ser refletidas nos mapeamentos JPA.
- O cálculo usa regras fixas e deve ter pesos e categorias avaliados com dados reais.
- A tabela `estoque_erp` ainda não filtra recomendações por disponibilidade.
- A chave composta dos itens de pedido pressupõe uma única linha de cada produto por pedido.
- A API não implementa autenticação ou autorização.
- Os endpoints de cadastro são voltados ao fluxo de ingestão V1; o projeto ainda pode evoluir com paginação completa, monitoramento, validação mais estrita e testes automatizados.
