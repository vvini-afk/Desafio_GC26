# Motor de recomendação

Projeto inicial do desafio GC, implementado com Spring Boot 4, Java 21, Spring Data JPA, Flyway e Lombok.

## Executar localmente

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, a aplicação usa um banco H2 em memória. O Flyway cria o schema ao iniciar; os dados são descartados quando a aplicação é encerrada.

Para usar PostgreSQL, defina as variáveis antes de iniciar:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/recomendacao"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sua-senha"
.\mvnw.cmd spring-boot:run
```

## Endpoint inicial

`GET /api/recomendacoes/{clienteId}` retorna os produtos ativos para um cliente existente. Retorna `404` quando o cliente não existe.

Esta é uma regra inicial provisória: cada produto ativo recebe score `1.0` e o motivo `Produto ativo`. O diagrama não define a fórmula nem os pesos; essa regra serve para deixar o fluxo HTTP e a persistência executáveis enquanto as regras de recomendação são implementadas.

A documentação OpenAPI fica disponível em `/swagger-ui.html`.

## APIs de contexto

O Open-Meteo é usado para localizar cidades brasileiras e consultar o clima atual. A BrasilAPI fornece os feriados nacionais e, quando informada, os feriados estaduais da UF.

- `GET /api/contexto/localizacao?cidade=São Paulo`
- `GET /api/contexto/clima?cidade=São Paulo`
- `GET /api/contexto/feriados?ano=2026`
- `GET /api/contexto/feriados?ano=2026&uf=SP`

Esses endpoints consultam as APIs externas no momento da requisição. A aplicação ainda não usa esses dados para alterar a pontuação das recomendações; o diagrama não define as regras ou pesos para essa influência.
