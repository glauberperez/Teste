# API de Cadastro de Pessoas

Prova técnica — API REST em Java 21 / Spring Boot 3.5 para cadastro de pessoas, com previsão de
nacionalidade a partir de uma API pública, autenticação via JWT e interface web para consumo.

---

## Como executar

Pré-requisito: **JDK 21** (nada além disso — o banco é em memória).

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

A aplicação sobe em <http://localhost:8080>.

| Recurso | URL |
| --- | --- |
| Interface web | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| Console do H2 | <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:cadastro`, usuário `sa`, sem senha) |

**Credenciais da API:** `admin` / `admin123`

A aplicação já sobe com 3 pessoas de exemplo cadastradas.

---

## Requisitos da prova e onde foram atendidos

| Requisito | Implementação |
| --- | --- |
| `POST /registrarName` — registra pessoa (documento, nome, sobrenome, e-mail) | `PessoaController.registrar` |
| `GET /list` — lista as pessoas | `PessoaController.listar` |
| `GET /list/{parâmetro}` — consulta uma pessoa | `PessoaController.buscar` — parâmetro = **documento** |
| `DELETE /list/{parâmetro}` — exclui uma pessoa | `PessoaController.excluir` |
| `GET /findNacionalityByPerson/{parâmetro}` — nacionalidade provável | `NacionalidadeController.prever` |
| Ao menos uma validação de tipo de dado **por API** | Ver seção *Validações* |
| Sistema de autenticação | JWT (`Bearer`) em **todas** as APIs de negócio — `SecurityConfig` |
| Interface web consumindo ao menos uma API | Página em `src/main/resources/static` (consome todas) |
| Sistema de persistência | H2 em memória + Spring Data JPA |

### Escolha do parâmetro

O parâmetro das rotas `/list/{...}` é o **documento (CPF)**. É o identificador natural da pessoa,
é estável e — diferente de um id sequencial — permite uma validação de formato real, que a prova
pede em cada endpoint.

---

## Endpoints

### 1. Autenticação

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","senha":"admin123"}'
```

```json
{ "token": "eyJhbGciOiJIUzM4NCJ9...", "tipo": "Bearer", "expiraEmSegundos": 7200 }
```

Guarde o token para as demais chamadas:

```bash
TOKEN="cole-o-token-aqui"
```

### 2. Registrar pessoa

```bash
curl -X POST http://localhost:8080/registrarName \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"documento":"52998224725","nome":"Nathaniel","sobrenome":"Silva","email":"nathaniel@exemplo.com"}'
```

`201 Created`, com header `Location: /list/52998224725`.

### 3. Listar pessoas

```bash
curl http://localhost:8080/list -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/list?limite=10" -H "Authorization: Bearer $TOKEN"
```

### 4. Consultar uma pessoa

```bash
curl http://localhost:8080/list/52998224725 -H "Authorization: Bearer $TOKEN"
```

### 5. Excluir uma pessoa

```bash
curl -X DELETE http://localhost:8080/list/52998224725 -H "Authorization: Bearer $TOKEN"
```

`204 No Content`.

### 6. Previsão de nacionalidade

```bash
curl http://localhost:8080/findNacionalityByPerson/52998224725 -H "Authorization: Bearer $TOKEN"
```

```json
{
  "documento": "52998224725",
  "nome": "Nathaniel",
  "nacionalidadeProvavel": { "codigoIso": "US", "pais": "Estados Unidos", "probabilidade": 0.09 },
  "outrasPossibilidades": [
    { "codigoIso": "GB", "pais": "Reino Unido", "probabilidade": 0.07 }
  ]
}
```

A API pública <https://api.nationalize.io> devolve apenas o **código ISO** (`US`). A prova pede o
**nome** da nacionalidade, então a conversão ISO → nome do país é feita pela aplicação, em
`NacionalidadeService.nomeDoPais`, usando `Locale` do próprio JDK — sem tabela hardcoded e sem
dependência extra.

---

## Validações

Cada endpoint tem pelo menos uma validação de tipo de dado, como exigido:

| Endpoint | Validações |
| --- | --- |
| `POST /registrarName` | `documento`: obrigatório + **CPF válido** (dígitos verificadores, módulo 11) · `nome`/`sobrenome`: obrigatórios, 2–60 caracteres, apenas letras · `email`: obrigatório e em formato de e-mail |
| `GET /list` | `limite`: inteiro entre 1 e 200 |
| `GET /list/{documento}` | `documento`: obrigatório + CPF válido |
| `DELETE /list/{documento}` | `documento`: obrigatório + CPF válido |
| `GET /findNacionalityByPerson/{documento}` | `documento`: obrigatório + CPF válido |
| `POST /auth/login` | `usuario`: obrigatório, até 60 caracteres · `senha`: obrigatória, 4–100 caracteres |

A validação de CPF é uma constraint customizada (`@Cpf` + `CpfValidator`), não apenas um regex:
ela confere os dígitos verificadores e rejeita sequências repetidas como `11111111111`.

**CPFs válidos para teste:** `52998224725`, `11144477735`, `39053344705`.

### Respostas de erro

Todos os erros saem no mesmo formato JSON:

```json
{
  "timestamp": "2026-08-31T18:04:11.52",
  "status": 400,
  "erro": "Requisicao invalida",
  "mensagem": "Um ou mais campos estao invalidos",
  "caminho": "/registrarName",
  "campos": { "documento": "documento invalido: informe um CPF valido (11 digitos, sem pontuacao)" }
}
```

| Status | Quando |
| --- | --- |
| `400` | Falha de validação (corpo, path ou query) |
| `401` | Sem token, token inválido ou expirado; credenciais erradas no login |
| `404` | Documento não encontrado |
| `409` | Documento ou e-mail já cadastrado |
| `503` | API pública de nacionalidade indisponível |

---

## Autenticação

- **Todas** as APIs de negócio exigem `Authorization: Bearer <token>`.
- O token é um JWT assinado com HMAC-SHA (HS384, escolhido pela jjwt conforme o tamanho da chave), com validade de 2 horas.
- Públicos apenas: `POST /auth/login`, os arquivos estáticos da interface, o Swagger e o console do H2.
- A senha do usuário fica em memória com hash BCrypt.

Usuário, senha e segredo do JWT são configuráveis por variáveis de ambiente
(`APP_USUARIO`, `APP_SENHA`, `APP_JWT_SECRET`) — os valores do `application.yml` são apenas defaults
de desenvolvimento.

---

## Interface web

Página única em HTML + JavaScript, servida pelo próprio Spring Boot (sem Node, sem build). Permite:

1. autenticar e obter o token;
2. registrar uma pessoa, com os erros de validação da API exibidos campo a campo;
3. listar as pessoas registradas;
4. consultar a nacionalidade provável de cada uma, com as probabilidades;
5. excluir uma pessoa.

O token fica em `sessionStorage` e é enviado no header `Authorization` a cada requisição.

---

## Testes

```bash
./mvnw test
```

- `CpfValidatorTest` — validação de CPF (casos válidos, dígito errado, tamanho, sequência repetida).
- `NacionalidadeServiceTest` — conversão ISO → nome do país, ordenação por probabilidade e ausência de previsão.
- `PessoaApiIntegrationTest` — todos os endpoints de ponta a ponta, passando pela cadeia real do
  Spring Security (401 sem token, 201, 400, 404, 409, 204).

A API pública **não** é chamada durante os testes: o cliente HTTP é substituído por um mock.

---

## Estrutura

```
src/main/java/com/quipux/cadastro
├── config/          RestClient, OpenAPI e carga inicial de dados
├── exception/       Exceções de negócio e handler global (@RestControllerAdvice)
├── nacionalidade/   Cliente da API pública, serviço e controller
├── pessoa/          Entidade, repositório, serviço, controller e DTOs
├── security/        JWT, filtro de autenticação e configuração do Spring Security
└── validation/      Constraint customizada @Cpf
src/main/resources/static   Interface web (HTML, CSS e JS)
```

---

## Decisões técnicas

- **H2 em memória**: a prova permite explicitamente armazenamento em memória, e assim o projeto roda
  com um único comando, sem instalar banco. Trocar por PostgreSQL exige apenas alterar o
  `application.yml` e a dependência — nenhuma mudança de código.
- **JWT em vez de HTTP Basic**: stateless, e é o formato que a interface web consome naturalmente.
- **Documento como parâmetro** das rotas: identificador natural e validável.
- **Timeouts explícitos** na chamada à API externa (5s), para que uma indisponibilidade de terceiro
  não prenda requisições da nossa API; a falha vira `503` com mensagem clara.
- **Conversão ISO → país via `Locale`**: usa a base de dados do próprio JDK, sem tabela manual.
- **Sem Lombok**: menos uma dependência para o avaliador precisar configurar na IDE.
