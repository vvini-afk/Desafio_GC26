# Motor de Recomendação

Projeto inicial desenvolvido para o desafio do **Geração Caldeira**, com o objetivo de construir a base de um motor de recomendação de produtos.

A aplicação foi desenvolvida em **Java 21** utilizando **Spring Boot 4.1.1**, com persistência através do Spring Data JPA e gerenciamento do banco de dados com Flyway.

O projeto está estruturado para permitir a evolução das regras de recomendação, inicialmente utilizando dados de produtos e clientes e, posteriormente, incorporando informações de contexto como **localização, clima e feriados**.

---

## Tecnologias utilizadas

* **Java 21** — linguagem principal do projeto.
* **Spring Boot 4.1.1** — framework utilizado para construção da aplicação.
* **Spring Web MVC** — criação dos endpoints REST.
* **Spring Data JPA** — persistência e acesso aos dados.
* **Flyway** — versionamento e criação das estruturas do banco de dados através de migrations.
* **PostgreSQL** — banco de dados suportado pela aplicação.
* **H2 Database** — banco em memória utilizado no ambiente inicial de desenvolvimento.
* **Bean Validation** — validação dos dados recebidos pela aplicação.
* **Lombok** — redução de código repetitivo nas classes Java.
* **Springdoc OpenAPI / Swagger UI** — documentação e exploração dos endpoints REST.
* **Maven** — gerenciamento de dependências e build do projeto.

As dependências e versões utilizadas estão definidas no `pom.xml` do projeto.

---

## Arquitetura inicial

A aplicação utiliza uma arquitetura baseada na separação de responsabilidades proporcionada pelo Spring.

De forma simplificada, o fluxo inicial da aplicação pode ser representado da seguinte maneira:

```text
                    ┌──────────────────────┐
                    │       Cliente        │
                    │  HTTP / REST Request │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │     Controller       │
                    │   Endpoints REST     │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │       Service        │
                    │   Regra de negócio   │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
       ┌──────────────────┐          ┌──────────────────┐
       │    Repository    │          │ APIs externas    │
       │   Spring Data    │          │ Open-Meteo /     │
       │       JPA        │          │ BrasilAPI        │
       └────────┬─────────┘          └──────────────────┘
                │
                ▼
       ┌──────────────────┐
       │     Banco de     │
       │      dados       │
       │ PostgreSQL / H2  │
       └──────────────────┘
```

Essa estrutura representa a base atual do projeto e permite que as regras de recomendação sejam implementadas gradualmente sem concentrar toda a responsabilidade em uma única camada.

### Persistência

O **Spring Data JPA** é responsável pela comunicação entre a aplicação e o banco de dados.

O **Flyway** é utilizado para controlar a evolução do schema por meio de migrations. Ao iniciar a aplicação no ambiente configurado com H2, o banco é criado em memória e as migrations são executadas.

No ambiente inicial, os dados do H2 são perdidos quando a aplicação é encerrada.

O projeto também possui dependência do driver do **PostgreSQL**, permitindo a utilização de um banco PostgreSQL para um ambiente persistente.

---

## Motor de recomendação

O endpoint principal do projeto é:

```http
GET /api/recomendacoes/{clienteId}
```

Ele recebe o identificador de um cliente e retorna os produtos ativos associados ao fluxo inicial de recomendação.

Atualmente, a regra de recomendação ainda é **provisória**:

* produtos ativos recebem `score = 1.0`;
* o motivo retornado é `Produto ativo`;
* caso o cliente não exista, a API retorna `404`.

Essa regra foi criada para manter o fluxo HTTP e a persistência funcionais enquanto as regras definitivas do motor de recomendação são desenvolvidas.

A fórmula de pontuação e os pesos das futuras regras ainda não estão definidos no projeto.

---

# APIs externas

O projeto possui uma camada inicial de **dados de contexto**, que futuramente poderá ser utilizada pelo motor de recomendação.

Atualmente existem três grupos de informações externas:

1. localização;
2. clima;
3. feriados.

Esses dados são consultados através de duas APIs externas:

* **Open-Meteo**
* **BrasilAPI**

Os endpoints de contexto realizam as consultas às APIs externas no momento da requisição.

> Importante: os dados retornados pelas APIs de contexto ainda **não alteram o score das recomendações**. A integração existe como base para a evolução futura das regras do motor.

---

## Open-Meteo

O projeto utiliza a **Open-Meteo** para obter informações relacionadas à localização e ao clima.

A Open-Meteo disponibiliza uma **Geocoding API**, capaz de pesquisar uma localização a partir do nome de uma cidade, além da API de previsão meteorológica. A plataforma não exige API key para seu uso não comercial.

### Localização

A aplicação possui o endpoint:

```http
GET /api/contexto/localizacao?cidade=São Paulo
```

A partir do nome da cidade informado, a integração com o serviço de geocoding da Open-Meteo permite obter informações de localização necessárias para as consultas meteorológicas.

O fluxo conceitual é:

```text
Nome da cidade
      │
      ▼
Open-Meteo Geocoding
      │
      ▼
Coordenadas geográficas
(latitude / longitude)
```

Essas coordenadas são importantes porque a consulta meteorológica da Open-Meteo utiliza latitude e longitude como referência para determinar as condições climáticas de uma localização.

### Clima

A aplicação também possui:

```http
GET /api/contexto/clima?cidade=São Paulo
```

O fluxo é baseado na localização da cidade:

```text
Cidade
  │
  ▼
Open-Meteo Geocoding
  │
  ▼
Latitude + Longitude
  │
  ▼
Open-Meteo Weather
  │
  ▼
Dados meteorológicos
```

A integração foi criada como uma fonte de contexto para o futuro motor de recomendação.

Por exemplo, uma futura regra poderia considerar condições climáticas para aumentar ou diminuir a relevância de determinados produtos. Essa lógica, entretanto, **ainda não está implementada no score atual**.

---

## BrasilAPI

A **BrasilAPI** é utilizada no projeto para consultar informações relacionadas a feriados.

O endpoint disponível é:

```http
GET /api/contexto/feriados?ano=2026
```

Também é possível informar uma UF:

```http
GET /api/contexto/feriados?ano=2026&uf=SP
```

A integração permite consultar:

* feriados nacionais;
* feriados estaduais quando uma UF é informada.

O fluxo conceitual é:

```text
Ano
 │
 ▼
BrasilAPI
 │
 ├── Feriados nacionais
 │
 └── Feriados estaduais
        │
        ▼
   Dados de contexto
```

Assim como acontece com os dados meteorológicos, os feriados ainda não interferem diretamente no cálculo do score.

A intenção é utilizar essas informações futuramente como uma variável adicional nas regras de recomendação, permitindo considerar períodos específicos do calendário na seleção e priorização de produtos.

---

# APIs de contexto

Atualmente, os endpoints relacionados às APIs externas são:

| Endpoint                                        | Finalidade                                                                      |
| ----------------------------------------------- | ------------------------------------------------------------------------------- |
| `GET /api/contexto/localizacao?cidade={cidade}` | Consulta a localização de uma cidade através da Open-Meteo                      |
| `GET /api/contexto/clima?cidade={cidade}`       | Consulta informações climáticas utilizando a localização obtida pela Open-Meteo |
| `GET /api/contexto/feriados?ano={ano}`          | Consulta os feriados nacionais através da BrasilAPI                             |
| `GET /api/contexto/feriados?ano={ano}&uf={UF}`  | Consulta feriados considerando também a UF informada                            |

Esses endpoints funcionam como uma primeira camada de integração de **contexto externo** para o motor de recomendação.

---

# Visão da evolução do motor

A arquitetura atual foi pensada para permitir que o motor evolua de uma regra simples para um sistema capaz de combinar diferentes fatores.

A ideia pode ser representada da seguinte forma:

```text
                 Dados do cliente
                       │
                       ▼
                Histórico de
                  compras
                       │
                       │
Dados de localização ──┤
                       │
Dados climáticos ──────┤
                       │
Dados de calendário ───┤
                       │
                       ▼
              Regras de recomendação
                       │
                       ▼
                  Cálculo do
                     score
                       │
                       ▼
             Produtos recomendados
```

O objetivo dessa estrutura é permitir que novas regras sejam adicionadas progressivamente.

Exemplos de fatores que poderão ser considerados no futuro:

* histórico de compras;
* perfil de consumo do cliente;
* localização;
* condições climáticas;
* período do ano;
* feriados;
* sazonalidade;
* combinação entre diferentes fatores.

Esses fatores ainda fazem parte da evolução planejada do motor e **não representam regras implementadas no score atual**.

---

## Executando o projeto

Para executar a aplicação localmente utilizando o Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, o projeto possui configuração para utilizar um banco **H2 em memória** no ambiente inicial.

O Flyway executa as migrations durante a inicialização da aplicação.

---

## Documentação da API

A aplicação utiliza **Springdoc OpenAPI** para disponibilizar a documentação dos endpoints.

Após iniciar a aplicação, a interface Swagger pode ser acessada em:

```text
/swagger-ui.html
```

Através dela é possível visualizar e testar os endpoints disponibilizados pela aplicação.

---

## Status atual

O projeto encontra-se em uma etapa inicial de desenvolvimento.

### Implementado

* Estrutura inicial da aplicação Spring Boot;
* Java 21;
* Persistência com Spring Data JPA;
* Migrations com Flyway;
* Suporte a H2 e PostgreSQL;
* Endpoint inicial de recomendações;
* Regra provisória de recomendação;
* Integração com Open-Meteo;
* Consulta de localização;
* Consulta de clima;
* Integração com BrasilAPI;
* Consulta de feriados nacionais e estaduais;
* Documentação OpenAPI / Swagger.

### Próximas evoluções

* Desenvolvimento das regras reais de recomendação;
* Definição dos pesos de cada fator;
* Utilização dos dados de clima, localização e calendário no score;
* Evolução do modelo de recomendação;
* Combinação de diferentes fatores de contexto;
* Aprimoramento da priorização dos produtos recomendados.

---

## Objetivo

O projeto serve como base para o desenvolvimento de um **motor de recomendação contextual**, capaz de combinar informações sobre clientes, produtos e fatores externos para gerar recomendações mais relevantes.

A arquitetura inicial prioriza uma base simples e evolutiva, permitindo que novas regras e fontes de dados sejam incorporadas conforme o desenvolvimento do projeto.
