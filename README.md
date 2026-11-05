# Motor de Recomendação Fruki

MVP de recomendação de bebidas Fruki para autoatendimento e apoio à força de vendas. A API Spring Boot consulta o histórico do cliente, catálogo, promoções, estoque, clima, feriados e região para montar recomendações explicáveis.

## O que o projeto entrega

- **Autoatendimento:** identifica o cliente por CNPJ, mostra recomendações, promoções e produtos disponíveis, e registra pedidos.
- **Painel do representante:** seleciona um representante e mostra carteira de clientes, clientes em quebra de consumo, clima e recomendações para a abordagem comercial.
- **Motor de recomendação:** pontua recompra, afinidade de família, cross-sell, promoção, clima, feriado e multiplicador regional.
- **Persistência:** PostgreSQL com JPA/Hibernate e migrations Flyway.

## Arquitetura

```text
HTML/CSS/JavaScript (src/main/resources/static)
                │ chamadas HTTP no mesmo servidor
                ▼
API REST (api: controllers e DTOs)
                │
                ▼
Serviços (recomendação, scoring, cadastro, contexto, cross-sell)
          ┌─────┴─────┐
          ▼           ▼
   Repositórios JPA   Integrações HTTP
          │           ├── Open-Meteo
          ▼           └── BrasilAPI
     PostgreSQL
```

Os principais pacotes Java são `api`, `service`, `model`, `repository` e `integration`. A interface é servida pelo próprio Spring Boot, sem servidor frontend separado.

## Tecnologias

- Java 21 e Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA, Hibernate e Bean Validation
- PostgreSQL e Flyway
- Springdoc OpenAPI/Swagger UI
- Maven Wrapper
- JUnit Jupiter e Mockito para testes unitários
- Open-Meteo para geocodificação e clima; BrasilAPI para CEP e feriados
- HTML, CSS e JavaScript para as telas

## Executar localmente

### Pré-requisitos

- JDK 21
- PostgreSQL acessível pela aplicação
- Acesso à internet para consultar CEP, clima e feriados

### Configurar o banco

Defina as configurações do seu banco no ambiente antes de iniciar. Exemplo no PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://<host>:5432/<database>"
$env:SPRING_DATASOURCE_USERNAME = "<usuario>"
$env:SPRING_DATASOURCE_PASSWORD = "<senha>"
```

Use valores próprios do seu ambiente. Não coloque credenciais reais no README, no repositório ou em exemplos compartilhados.

O Hibernate está configurado com `ddl-auto=validate`: valida o mapeamento Java contra o banco e não cria nem altera tabelas. O Flyway usa `src/main/resources/db/migration` e o schema `public`.

#### Migrations

| Migration | O que faz |
|---|---|
| `V1__criar_esquema_inicial.sql` | Cria as tabelas principais em um banco vazio: clientes, cidades, hierarquia, produtos, estoque, pedidos, itens, complementares, promoções e notificações. |
| `V2__adicionar_regiao_e_multiplicador_em_cidade.sql` | Adiciona `regiao` e `multiplicador` à tabela `cidade`, se ainda não existirem. |
| `V3__criar_view_clientes_quebra_consumo.sql` | Cria a view `public.vw_clientes_quebra_consumo`, usada na identificação de clientes com quebra de consumo. |

Em banco vazio, o Flyway executa as três migrations. Para um schema existente sem histórico Flyway, a configuração atual cria baseline na versão 1 e executa as migrations posteriores; o banco existente precisa ser compatível com a estrutura esperada pela V1. Não edite migrations já aplicadas nem altere manualmente o histórico do Flyway para contornar erros de schema.

O catálogo precisa ter produtos e linhas em `estoque_erp`. O motor e a API de catálogo mostram apenas produtos ativos com quantidade disponível maior que zero. Para aplicar o fator regional, preencha `cidade.regiao` e `cidade.multiplicador`.

### Iniciar

Na raiz do projeto:

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Linux/macOS
./mvnw spring-boot:run
```

A aplicação fica em `http://localhost:8080`. A interface Swagger está em `http://localhost:8080/swagger-ui/index.html` e a especificação OpenAPI em `http://localhost:8080/v3/api-docs`.

### Telas

- `http://localhost:8080/` ou `/index.html`: autoatendimento do cliente.
- `http://localhost:8080/vendedor.html`: painel da força de vendas.
- `/vendedor.html?codRepresentante={codigo}`: abre o painel de um representante específico.

O painel carrega os representantes do banco e permite trocar a seleção. Se nenhum código for informado, usa o representante padrão `31906` quando existir; caso contrário, seleciona o primeiro disponível.

## Endpoints

### Clientes e força de vendas

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/clientes` | Lista até 50 clientes, em ordem de ID. |
| `GET` | `/api/clientes?codRepresentante={codigo}` | Lista a carteira do representante. |
| `GET` | `/api/clientes/busca?cnpj={cnpj}` | Procura cliente pelo CNPJ, com ou sem formatação. |
| `GET` | `/api/clientes/vendedores` | Lista representantes disponíveis para o painel. |
| `GET` | `/api/clientes/vendedor/{codRepresentante}` | Retorna indicadores e carteira do representante. |
| `GET` | `/api/clientes/quebra-consumo` | Lista clientes identificados pela view de quebra de consumo. |
| `POST` | `/api/clientes` | Cadastra cliente. A cidade deve existir; pode ser informada por `cidadeId` ou nome. |

### Catálogo, pedidos e promoções

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/produtos/catalogo` | Retorna produtos ativos com estoque positivo e dados da promoção vigente aplicável. |
| `POST` | `/api/produtos` | Cadastra produto e, opcionalmente, produtos complementares. |
| `POST` | `/api/pedidos` | Registra pedido e itens; ao final recalcula as associações de cross-sell. |
| `POST` | `/api/promocoes` | Cadastra promoção por produto ou família. Tipos: `PERCENTUAL` e `VALOR_FIXO`. |

### Recomendação e contexto

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/recomendacoes/{clienteId}?limite=10` | Retorna recomendações ordenadas pelo score. Aceita `cidade` opcional e `limite` entre 1 e 100. |
| `GET` | `/api/recomendacoes/{clienteId}/diagnostico?limite=10` | Retorna recomendações e resumo de localização, região, multiplicador, recompras e cross-sells. |
| `GET` | `/api/recomendacoes/quebra-consumo?limiteClientes=10&limitePorCliente=10` | Gera recomendações para clientes na view. Limite de até 25 clientes e 100 recomendações por cliente. |
| `POST` | `/api/admin/processar-cross-sell` | Recalcula e persiste os produtos complementares com base nos pedidos existentes. |
| `GET` | `/api/contexto/clima?cidade={cidade}&uf={UF}&cep={CEP}` | Consulta clima atual. Cidade e UF são usadas como alternativa se o CEP não fornecer coordenadas. |
| `GET` | `/api/contexto/localizacao?cidade={cidade}` | Consulta coordenadas e região geográfica da cidade. |
| `GET` | `/api/contexto/feriados?ano={ano}&uf={UF}` | Lista feriados; UF é opcional. |

Todos os `POST` recebem JSON com `Content-Type: application/json`. Exemplos de corpos:

```json
{
  "cnpj": "00000000000000",
  "nome": "Cliente de Teste",
  "perfil": "Varejo",
  "subsegmento": "Mercado",
  "cep": "90000000",
  "cidade": "Porto Alegre"
}
```

```json
{
  "clienteId": 1,
  "dataCompra": "2026-11-05",
  "itens": [
    { "produtoId": 1, "quantidade": 2, "valorUnitario": 45.00 }
  ]
}
```

Use IDs existentes no banco para cliente, cidade, produto, representante e promoção. O exemplo é ilustrativo; o pedido só será gravado se as referências existirem e os dados passarem pela validação.

## Como funciona o score

O score parte de 10 pontos, mas esse valor sozinho não gera recomendação. Produtos sem nenhum sinal aplicável são removidos e a resposta fica vazia se não restar candidato elegível.

| Fator | Regra atual |
|---|---|
| Recompra direta | `+5` se o produto exato já aparece no histórico do cliente. |
| Afinidade de família | `+3` por item comprado da mesma família, limitado a `+10`; usado para produto ainda não comprado. |
| Cross-sell inédito | `+20` se o produto complementar está associado a um item comprado e ainda não foi comprado pelo cliente. |
| Promoção vigente | `+15`; promoção específica do produto tem prioridade sobre promoção por família. |
| Clima quente | `+8` para famílias configuradas quando temperatura atinge o limite. |
| Clima frio | `+10` para famílias configuradas quando temperatura fica no limite ou abaixo. |
| Chuva | `+6` para famílias configuradas quando precipitação ou código WMO indica chuva. |
| Feriado próximo | `+7` para famílias configuradas dentro da janela definida. |
| Região | Multiplica o score pelo valor de `cidade.multiplicador`; valor ausente ou inválido equivale a `1`. |

Os limites e famílias de clima/feriado ficam em `application.properties` e podem ser substituídos pelas variáveis `REC_*`. Os valores padrão atuais são 24 °C para calor, 18 °C para frio, 0,5 mm para chuva e janela de sete dias para feriados. As famílias configuradas são hipóteses comerciais baseadas no catálogo, não resultados de validação estatística.

O DTO de recomendação inclui motivos, preço base e mínimo, desconto, promoção, flags `jaComprado`, `emPromocao`, `isCrossSell` e a lista `pilares`. A consulta do clima também aparece nos motivos; ela só pontua quando a condição e a família correspondem às regras configuradas.

O cross-sell calcula frequência e confiança de pares de produtos no mesmo pedido e persiste até três complementares por produto. O recálculo ocorre pelo endpoint administrativo e também após o registro de um pedido.

## Quebra de consumo e notificações

A migration V3 cria `vw_clientes_quebra_consumo`. A view estima o intervalo médio entre compras para clientes com pelo menos três pedidos e marca quebra quando os dias sem comprar ultrapassam 1,5 vez esse intervalo. A tela do representante usa esses dados para sinalizar clientes prioritários.

A entidade e o repositório de notificações estão mapeados, incluindo o tipo `RECUPERACAO`. Ainda não há endpoint ou serviço que gere e envie mensagens automaticamente.

## Testes

Os testes unitários atuais cobrem pontuação contextual, motor de recomendação e cálculo de cross-sell. Para executá-los:

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## Limitações conhecidas

- Os pesos e as regras de scoring são fixos/configuráveis, mas ainda precisam ser avaliados com resultados reais de vendas.
- As associações de clima e feriados às famílias são hipóteses comerciais.
- O filtro de estoque depende da atualização de `estoque_erp`; o sistema não reserva nem sincroniza saldo com o ERP.
- O recálculo de cross-sell é síncrono no registro de pedido, o que pode aumentar o tempo de resposta conforme o histórico cresce.
- Clima, geocodificação e feriados dependem de serviços externos e não têm cache local.
- A lista geral de clientes tem limite fixo de 50; não há paginação configurável.
- A API ainda não implementa autenticação/autorização.
- Notificações têm persistência mapeada, mas não têm fluxo de geração/envio.
