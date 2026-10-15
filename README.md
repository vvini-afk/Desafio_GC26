# Motor de Recomendação

Projeto desenvolvido para o desafio do **Geração Caldeira**, com o objetivo de construir a base de um motor de recomendação de produtos.

A aplicação utiliza **Java 21**, **Spring Boot 4.1.1**, **Spring Data JPA** e **PostgreSQL** para persistência dos dados.

O projeto evoluiu da estrutura inicial para uma aplicação conectada a um banco de dados real. Também foi criada uma controller de teste para consultar os clientes cadastrados e retornar seus dados em formato JSON, permitindo validar a comunicação entre a aplicação e o banco.

A arquitetura está preparada para a evolução das regras de recomendação, combinando informações de clientes, produtos e histórico de compras com dados de contexto, como localização, clima e feriados.

---

## Tecnologias utilizadas

- **Java 21** — linguagem principal do projeto.
- **Spring Boot 4.1.1** — framework para construção da aplicação.
- **Spring Web MVC** — criação dos endpoints REST.
- **Spring Data JPA** — persistência e acesso aos dados.
- **PostgreSQL** — banco de dados utilizado pela aplicação.
- **Bean Validation** — validação dos dados recebidos pela aplicação.
- **Lombok** — redução de código repetitivo nas classes Java.
- **Springdoc OpenAPI / Swagger UI** — documentação e exploração dos endpoints REST.
- **Maven** — gerenciamento de dependências e build do projeto.

As dependências e versões utilizadas estão definidas no `pom.xml`.

O projeto utiliza o Spring Data JPA para realizar as operações de persistência. O Flyway, utilizado na estrutura inicial, foi removido após a integração com o banco de dados.

---

## Arquitetura da aplicação

A aplicação utiliza uma arquitetura baseada na separação de responsabilidades proporcionada pelo Spring.

O fluxo de comunicação entre as principais camadas pode ser representado da seguinte maneira:

```text
                 Cliente HTTP / REST
                         |
                         v
                    Controller
                         |
                         v
                      Service
                         |
             +-----------+-----------+
             |                       |
             v                       v
         Repository             APIs externas
       Spring Data JPA       Open-Meteo / BrasilAPI
             |
             v
        Banco de dados
         PostgreSQL
```

### Responsabilidades das camadas

- **Controller:** recebe as requisições HTTP e disponibiliza os endpoints da API.
- **Service:** concentra as regras de negócio e a lógica da aplicação.
- **Repository:** realiza o acesso aos dados utilizando o Spring Data JPA.
- **Banco de dados:** armazena os dados persistentes utilizados pela aplicação.
- **APIs externas:** fornecem informações complementares de localização, clima e calendário.

Essa organização facilita a manutenção do código e permite adicionar novas funcionalidades sem concentrar todas as responsabilidades em uma única classe.

---

## Persistência de dados

O projeto utiliza **PostgreSQL hospedado no Supabase**, integrado à aplicação por meio do Spring Data JPA.

As entidades Java representam os dados manipulados pela aplicação, enquanto os repositórios realizam as operações de consulta e persistência.

Após a integração com o banco de dados, o Flyway foi removido do projeto. A aplicação passou a utilizar a conexão com o PostgreSQL do Supabase para acessar os dados persistidos.

As configurações de conexão devem ser definidas de acordo com as propriedades utilizadas pelo projeto. As credenciais do banco de dados não devem ser armazenadas diretamente no código-fonte nem enviadas ao GitHub.

### Consulta de clientes

Foi criada uma controller de teste para validar a comunicação entre a aplicação Spring Boot e o banco de dados.

**Endpoint disponível:**

```http
GET http://localhost:8080/teste/cliente
```

A requisição consulta os clientes armazenados no banco de dados do Supabase e retorna os dados em formato JSON.

Essa implementação permite verificar se a aplicação consegue recuperar os registros por meio da camada de persistência e disponibilizá-los através de uma API REST.

O endpoint tem finalidade de teste e validação da integração com o banco de dados.

### Consulta de clientes

Foi criada uma controller de teste para verificar a integração entre a API e o banco de dados.

Essa funcionalidade permite consultar os clientes cadastrados e retornar os registros em formato JSON, facilitando a validação dos dados recuperados pela aplicação.

O fluxo da consulta é:

```text
Requisição HTTP
      |
      v
Controller de teste
      |
      v
Repository JPA
      |
      v
Banco de dados
      |
      v
Lista de clientes
      |
      v
Resposta JSON
```

Essa implementação funciona como uma etapa de validação da persistência e da exposição dos dados por meio da API REST.

> **Observação:** o caminho exato do endpoint de teste deve ser documentado conforme o mapeamento definido na controller.

---

## Motor de recomendação

O endpoint principal do motor de recomendação é:

```http
GET /api/recomendacoes/{clienteId}
```

Ele recebe o identificador de um cliente e retorna os produtos ativos associados ao fluxo de recomendação.

### Hipótese e regra inicial (V1)

A regra inicial considera que clientes podem apresentar interesse por produtos de categorias que já compraram, por produtos complementares aos itens do histórico e por ofertas vigentes.

Esses fatores são utilizados para calcular um score explicável, sem depender de um modelo de inteligência artificial treinado.

Os produtos inativos são excluídos. Cada produto ativo recebe 10 pontos de base, e o ranking considera os seguintes sinais:

| Sinal | Pontuação | Justificativa |
|---|---:|---|
| Cada item comprado na mesma categoria | +5, limitado a +25 | Representa uma preferência observada do cliente. |
| Complementaridade com algum produto comprado | +20 | Considera relações de complementaridade cadastradas nos produtos. |
| Promoção ativa para o produto ou sua categoria | +15 | Destaca ofertas vigentes no momento da consulta. |

A lista é ordenada do maior para o menor score. Cada recomendação apresenta os motivos que contribuíram para sua pontuação.

Em caso de empate, os produtos são ordenados pelo identificador. Na ausência de histórico ou promoção, o produto ativo mantém a pontuação-base de 10 pontos, com essa condição indicada no motivo. Um cliente inexistente retorna `404`.

Os dados de clima e feriados permanecem como informações de contexto e ainda não interferem no cálculo do score V1.

### Endpoints de cadastro

Os endpoints abaixo fazem parte do fluxo de alimentação dos dados utilizados pelo motor:

- `POST /api/clientes` — cadastra um cliente.
- `POST /api/produtos` — cadastra um produto. O campo `produtosComplementaresIds` é opcional e referencia produtos já cadastrados.
- `POST /api/pedidos` — registra uma compra e seus itens. As quantidades devem ser positivas.
- `POST /api/promocoes` — cadastra uma promoção por produto ou categoria, com tipo, valor e período.

Exemplo de inclusão de compra:

```json
{
  "clienteId": 1,
  "dataCompra": "2026-10-14",
  "itens": [
    {
      "produtoId": 2,
      "quantidade": 1
    }
  ]
}
```

Exemplo de consulta das recomendações:

```http
GET /api/recomendacoes/1
```

---

# APIs externas

O projeto possui uma camada de integração com fontes externas de dados de contexto, que poderão ser utilizadas futuramente pelo motor de recomendação.

As integrações contemplam três grupos de informações:

1. Localização.
2. Clima.
3. Feriados.

As APIs utilizadas são:

- **Open-Meteo** — localização e informações meteorológicas.
- **BrasilAPI** — informações relacionadas a feriados.

Os dados retornados por essas integrações ainda não alteram o score das recomendações. A implementação atual fornece uma base para futuras regras de negócio.

---

## Open-Meteo

A Open-Meteo é utilizada para obter informações de localização e clima.

Sua Geocoding API permite pesquisar localidades a partir do nome de uma cidade, enquanto a API meteorológica utiliza coordenadas geográficas para consultar informações sobre as condições climáticas.

### Localização

```http
GET /api/contexto/localizacao?cidade=São Paulo
```

O endpoint consulta a localização informada utilizando a integração com o serviço de geocoding.

Fluxo conceitual:

```text
Nome da cidade
      |
      v
Open-Meteo Geocoding
      |
      v
Latitude e longitude
```

### Clima

```http
GET /api/contexto/clima?cidade=São Paulo
```

O fluxo utiliza a localização obtida para realizar a consulta meteorológica:

```text
Cidade
  |
  v
Open-Meteo Geocoding
  |
  v
Latitude e longitude
  |
  v
Open-Meteo Weather
  |
  v
Dados meteorológicos
```

Essas informações poderão servir de base para regras futuras, como a priorização de produtos associados a determinadas condições climáticas. Essa lógica ainda não faz parte do score atual.

---

## BrasilAPI

A BrasilAPI é utilizada para consultar informações relacionadas a feriados.

Consulta de feriados nacionais:

```http
GET /api/contexto/feriados?ano=2026
```

Consulta considerando uma unidade federativa:

```http
GET /api/contexto/feriados?ano=2026&uf=SP
```

A integração permite consultar feriados nacionais e, quando uma UF é informada, feriados estaduais conforme os dados disponibilizados pela API.

Fluxo conceitual:

```text
Ano e UF opcional
        |
        v
    BrasilAPI
        |
        v
Dados de feriados
        |
        v
Dados de contexto
```

Essas informações poderão ser utilizadas futuramente para considerar datas comemorativas, períodos específicos do calendário e sazonalidade nas recomendações.

---

## APIs de contexto

Os endpoints relacionados às integrações externas são:

| Endpoint | Finalidade |
|---|---|
| `GET /api/contexto/localizacao?cidade={cidade}` | Consulta a localização de uma cidade utilizando a Open-Meteo. |
| `GET /api/contexto/clima?cidade={cidade}` | Consulta informações climáticas com base na localização. |
| `GET /api/contexto/feriados?ano={ano}` | Consulta feriados nacionais pela BrasilAPI. |
| `GET /api/contexto/feriados?ano={ano}&uf={UF}` | Consulta feriados considerando a UF informada. |

Esses endpoints compõem a camada de integração com dados externos da aplicação.

---

## Evolução do motor de recomendação

A arquitetura foi planejada para permitir que o motor evolua gradualmente, incorporando diferentes fatores na seleção e priorização de produtos.

```text
              Dados do cliente
                    |
                    v
              Histórico de
                compras
                    |
                    |
Dados de localização
                    |
Dados climáticos
                    |
Dados de calendário
                    |
                    v
          Regras de recomendação
                    |
                    v
             Cálculo do score
                    |
                    v
           Produtos recomendados
```

Entre os fatores que poderão ser considerados nas próximas etapas estão:

- Histórico de compras.
- Perfil de consumo do cliente.
- Localização.
- Condições climáticas.
- Período do ano.
- Feriados.
- Sazonalidade.
- Combinação entre diferentes fatores.

Essas possibilidades fazem parte da evolução planejada. A utilização de cada variável no cálculo do score dependerá da definição e da validação das respectivas regras de negócio.

---

## Executando o projeto

Para executar a aplicação localmente utilizando o Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Antes de iniciar a aplicação, verifique se:

- O Java 21 está instalado e configurado.
- As dependências do projeto podem ser resolvidas pelo Maven.
- O banco de dados está acessível.
- As configurações de conexão estão definidas corretamente nas propriedades da aplicação ou nas variáveis de ambiente utilizadas pelo projeto.

A aplicação utiliza o banco de dados configurado, em vez de depender do banco H2 em memória descrito na estrutura inicial.

---

## Documentação da API

A aplicação utiliza Springdoc OpenAPI para documentar os endpoints REST.

Após iniciar a aplicação, a interface Swagger pode ser acessada em:

```text
/swagger-ui.html
```

Por meio dela, é possível visualizar e testar os endpoints disponibilizados pela aplicação, conforme as configurações do Springdoc.

---

## Status atual

O projeto encontra-se em desenvolvimento, com a estrutura inicial implementada e a integração com o banco de dados estabelecida.

### Implementado

- Estrutura inicial da aplicação Spring Boot.
- Desenvolvimento em Java 21.
- Persistência com Spring Data JPA.
- Integração com o banco de dados.
- Controller de teste para consulta de clientes em JSON.
- Endpoint de recomendações com ranking por histórico, produtos complementares e promoções vigentes, conforme a implementação V1 documentada.
- Cadastro de clientes, produtos, compras e promoções.
- Integração com Open-Meteo.
- Consulta de localização.
- Consulta de clima.
- Integração com BrasilAPI.
- Consulta de feriados nacionais e estaduais.
- Documentação OpenAPI / Swagger.

### Alterações recentes

- Integração do banco de dados ao projeto.
- Remoção do Flyway e de sua configuração de migrations.
- Criação de uma controller de teste para retornar os dados dos clientes cadastrados em formato JSON.

### Próximas evoluções

- Validar os pesos do score V1 com dados reais.
- Definir relações de negócio justificadas entre clima, localização, calendário e categorias de produtos.
- Avaliar a qualidade das recomendações.
- Aprimorar a priorização de produtos conforme o perfil e o contexto dos clientes.

---

## Objetivo

O objetivo do projeto é desenvolver um **motor de recomendação contextual** capaz de combinar informações sobre clientes, produtos, histórico de compras e fatores externos para gerar recomendações mais relevantes.

A implementação atual estabelece uma base com persistência de dados, endpoints REST e integrações externas, permitindo evoluir progressivamente as regras de recomendação e as funcionalidades da aplicação.
