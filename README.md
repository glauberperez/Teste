# API de Cadastro de Pessoas

Prova técnica — Java 21 / Spring Boot 3.5, H2 em memória, autenticação JWT e interface web.

## Rodar

Precisa apenas do **JDK 21**:

```bash
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # Linux / macOS
```

Sobe em `http://localhost:8080` já com 3 pessoas cadastradas.

**Login:** `admin` / `admin123`

## URLs para testar

| | |
| --- | --- |
| Interface web | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Console do H2 | http://localhost:8080/h2-console |

No Swagger: faça `POST /auth/login`, copie o `token` e cole no botão **Authorize**.
No H2: JDBC URL `jdbc:h2:mem:cadastro`, usuário `sa`, senha em branco.

**CPFs válidos para teste:** `52998224725` · `11144477735` · `39053344705` · `12345678909`

## Rotas

Todas exigem `Authorization: Bearer <token>`, exceto o login.

| Método | Rota | O que faz | Validação |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Devolve o token JWT (validade de 2h) | usuário e senha obrigatórios, tamanho mínimo |
| `POST` | `/registrarName` | Cadastra pessoa: documento, nome, sobrenome e e-mail | CPF válido, e-mail em formato válido, nome só com letras |
| `GET` | `/list` | Lista as pessoas cadastradas | `?limite=` entre 1 e 200 |
| `GET` | `/list/{documento}` | Dados de uma pessoa | CPF válido |
| `DELETE` | `/list/{documento}` | Exclui uma pessoa | CPF válido |
| `GET` | `/findNacionalityByPerson/{documento}` | Nacionalidade provável, via `api.nationalize.io` | CPF válido |

O parâmetro escolhido para identificar a pessoa é o **documento (CPF)**.

A API pública devolve só o código ISO (`US`); a aplicação converte para o nome do país
(`Estados Unidos`) usando `Locale` do JDK.

### Exemplos

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","senha":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl http://localhost:8080/list -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8080/registrarName \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"documento":"12345678909","nome":"Nathaniel","sobrenome":"Silva","email":"n@exemplo.com"}'

curl http://localhost:8080/findNacionalityByPerson/12345678909 -H "Authorization: Bearer $TOKEN"

curl -X DELETE http://localhost:8080/list/12345678909 -H "Authorization: Bearer $TOKEN"
```

## Respostas de erro

Formato único em todos os casos, sem vazar stack trace:

```json
{
  "timestamp": "2026-08-31T18:04:11.52",
  "status": 400,
  "erro": "Requisicao invalida",
  "mensagem": "Um ou mais campos estao invalidos",
  "caminho": "/registrarName",
  "campos": { "documento": "documento invalido: informe um CPF valido" }
}
```

`400` validação · `401` sem token ou credenciais erradas · `404` não encontrado ·
`405` método errado · `409` documento ou e-mail duplicado · `415` content-type errado ·
`503` API de nacionalidade fora do ar

## Testes

```bash
mvnw.cmd test
```

39 testes: validação de CPF, conversão ISO → país e todos os endpoints de ponta a ponta
passando pela cadeia real do Spring Security. A API pública não é chamada nos testes.

## Estrutura

```
src/main/java/com/quipux/cadastro
├── config/          RestClient, OpenAPI e carga inicial
├── exception/       Exceções e handler global
├── nacionalidade/   Cliente da API pública, serviço e controller
├── pessoa/          Entidade, repositório, serviço, controller e DTOs
├── security/        JWT e configuração do Spring Security
└── validation/      Constraint customizada @Cpf
src/main/resources/static   Interface web (HTML, CSS e JS)
```

A validação de CPF confere os dígitos verificadores (módulo 11), não é apenas um regex.
Usuário, senha e segredo do JWT são sobrescrevíveis por `APP_USUARIO`, `APP_SENHA` e
`APP_JWT_SECRET`.
