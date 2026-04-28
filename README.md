# Task Manager

Sistema de gerenciamento de tarefas para equipes de desenvolvimento, com autenticação JWT, controle de acesso por papel (ADMIN/MEMBER), CRUD de projetos e tarefas, regras de negócio (transição de status, WIP limit, fechamento de tarefas críticas), busca textual, filtros, paginação e relatório resumido por projeto.

**Stack**

| Camada      | Tecnologia                                            |
|-------------|-------------------------------------------------------|
| Backend     | Java 17 · Spring Boot 3.3 · Spring Security · JPA     |
| Banco       | PostgreSQL 16 (prod) · H2 PostgreSQL-mode (test)      |
| Migrations  | Flyway                                                |
| Frontend    | React 18 · TypeScript · Vite · React Router 6         |
| Testes      | JUnit 5 · Mockito · MockMvc · Vitest · Testing Library |
| Docs API    | springdoc-openapi (Swagger UI)                        |
| Container   | Docker Compose (Postgres)                             |

---

## Sumário

- [Como rodar](#como-rodar)
- [Banco de dados (Docker)](#banco-de-dados-docker)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Endpoints principais](#endpoints-principais)
- [Regras de negócio](#regras-de-negócio)
- [Arquitetura](#arquitetura)
- [Decisões técnicas e tradeoffs](#decisões-técnicas-e-tradeoffs)
- [Estratégia de testes](#estratégia-de-testes)
- [Estrutura de pastas](#estrutura-de-pastas)

---

## Como rodar

### Pré-requisitos

- Java 17+
- Maven 3.9+
- Node.js 18+ (apenas para rodar o frontend)
- Docker + Docker Compose (recomendado para o banco)

### Passo a passo

```bash
# 1. Subir o Postgres
docker compose up -d

# 2. Backend (porta 8080)
cd backend
mvn spring-boot:run

# 3. Frontend (porta 5173)
cd ../frontend
npm install
npm run dev
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Frontend: <http://localhost:5173> (proxy `/v1/*` → backend:8080)

---

## Banco de dados (Docker)

O `docker-compose.yml` na raiz sobe um PostgreSQL 16 Alpine com healthcheck, volume persistente e o banco já criado.

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: task-manager-postgres
    environment:
      POSTGRES_DB: taskmanager
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5434:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d taskmanager"]
      interval: 5s
      timeout: 5s
      retries: 5
```

| Item              | Valor                          |
|-------------------|--------------------------------|
| Imagem            | `postgres:16-alpine`           |
| Container         | `task-manager-postgres`        |
| Porta host        | `5434` (mapeada para `5432`)   |
| Database          | `taskmanager`                  |
| Usuário           | `postgres`                     |
| Senha             | `postgres`                     |
| Volume            | `postgres_data` (named volume) |
| Healthcheck       | `pg_isready` a cada 5s         |

**Comandos úteis:**

```bash
docker compose up -d          # sobe em background
docker compose ps             # status
docker compose logs -f postgres
docker compose down           # para os containers (mantém o volume)
docker compose down -v        # para e apaga o volume (zera os dados)
```

> A porta **5434** foi escolhida para não colidir com instâncias locais de Postgres rodando em 5432/5433. Para usar 5432, edite o mapeamento em `docker-compose.yml` e ajuste `DB_PORT=5432`.

**Sem Docker** (Postgres já instalado):

```bash
psql -U postgres -c "CREATE DATABASE taskmanager;"
```

As migrations Flyway (`V1`, `V2`, `V3` em `backend/src/main/resources/db/migration`) rodam automaticamente no startup do Spring Boot.

---

## Variáveis de ambiente

Todas têm default de desenvolvimento — não é necessário criar `.env` para rodar local.

| Variável       | Default                                | Descrição                          |
|----------------|----------------------------------------|------------------------------------|
| `DB_HOST`      | `localhost`                            | Host do Postgres                   |
| `DB_PORT`      | `5434`                                 | Porta (alinhada com docker-compose) |
| `DB_NAME`      | `taskmanager`                          | Nome do banco                      |
| `DB_USERNAME`  | `postgres`                             | Usuário                            |
| `DB_PASSWORD`  | `postgres`                             | Senha                              |
| `JWT_SECRET`   | valor de dev (ver `application.yml`)   | **Trocar em produção**             |
| `JWT_EXPIRATION_MS` | `86400000` (24h)                  | Validade do token                  |

---

## Endpoints principais

| Método | Path                                    | Descrição                                  | Autorização       |
|--------|-----------------------------------------|--------------------------------------------|-------------------|
| POST   | `/v1/auth/register`                     | Cria usuário (sempre como `MEMBER`)        | Público           |
| POST   | `/v1/auth/login`                        | Retorna JWT                                | Público           |
| POST   | `/v1/auth/admins`                       | Cria outro usuário com role `ADMIN`        | ADMIN logado      |
| POST   | `/v1/projects`                          | Cria projeto                               | ADMIN             |
| GET    | `/v1/projects`                          | Lista projetos do usuário                  | Autenticado       |
| GET    | `/v1/projects/{id}`                     | Detalhe                                    | Membro            |
| PUT    | `/v1/projects/{id}`                     | Atualiza                                   | Owner / ADMIN     |
| DELETE | `/v1/projects/{id}`                     | Remove                                     | Owner / ADMIN     |
| POST   | `/v1/projects/{id}/members`             | Adiciona membro                            | Owner / ADMIN     |
| DELETE | `/v1/projects/{id}/members/{userId}`    | Remove membro                              | Owner / ADMIN     |
| POST   | `/v1/projects/{id}/tasks`               | Cria tarefa                                | Membro            |
| GET    | `/v1/projects/{id}/tasks`               | Lista (filtros + busca + paginação)        | Membro            |
| GET    | `/v1/projects/{id}/tasks/{taskId}`      | Detalhe                                    | Membro            |
| PUT    | `/v1/projects/{id}/tasks/{taskId}`      | Atualiza                                   | Membro            |
| DELETE | `/v1/projects/{id}/tasks/{taskId}`      | Remove                                     | Membro            |
| GET    | `/v1/projects/{id}/report`              | Resumo por status e prioridade             | Membro            |

### Filtros, busca e ordenação

`GET /v1/projects/{id}/tasks` aceita os seguintes query params (todos opcionais e combináveis):

```
status=TODO|IN_PROGRESS|DONE
priority=LOW|MEDIUM|HIGH|CRITICAL
assigneeId=123
deadlineFrom=2026-04-01T00:00:00Z
deadlineTo=2026-04-30T23:59:59Z
search=texto                  # busca em título e descrição (case-insensitive)
page=0&size=20                # paginação
sort=priority,desc            # também: createdAt, deadline
```

Resposta paginada com metadata:

```json
{
  "content": [ /* TaskResponse[] */ ],
  "page": 0,
  "size": 20,
  "total": 47,
  "totalPages": 3
}
```

### Exemplo de relatório

```http
GET /v1/projects/10/report
```

```json
{
  "projectId": 10,
  "byStatus":   { "TODO": 12, "IN_PROGRESS": 3, "DONE": 45 },
  "byPriority": { "LOW": 30, "MEDIUM": 18, "HIGH": 10, "CRITICAL": 2 }
}
```

### Tratamento de erros (RFC 7807)

Todas as respostas de erro seguem o padrão `ProblemDetail`:

```json
{
  "type": "https://taskmanager/errors/wip-limit-exceeded",
  "title": "wip-limit-exceeded",
  "status": 422,
  "detail": "O responsável já possui 5 tarefas IN_PROGRESS (limite: 5)",
  "timestamp": "2026-04-28T14:00:00Z"
}
```

| Status | Quando                                                |
|--------|-------------------------------------------------------|
| 400    | Validação de input (Bean Validation)                  |
| 401    | Não autenticado / token inválido                      |
| 403    | Autenticado mas sem permissão                         |
| 404    | Recurso não encontrado                                |
| 409    | Conflito (ex.: e-mail duplicado)                      |
| 422    | Regra de negócio violada (transição, WIP, CRITICAL)   |

Mensagens de validação e de regra de negócio são retornadas em **português**.

---

## Regras de negócio

Implementadas em [`TaskService`](backend/src/main/java/com/taskmanager/application/TaskService.java):

1. **Transição de status** — `DONE` só pode voltar para `IN_PROGRESS` (nunca para `TODO`). Mapa de transições em [`Status.java`](backend/src/main/java/com/taskmanager/domain/task/Status.java).
2. **CRITICAL → DONE** — somente o **owner do projeto** ou usuário com role global **ADMIN** pode fechar tarefas críticas.
3. **WIP limit** — cada responsável pode ter no máximo **5 tarefas IN_PROGRESS** simultaneamente. Mensagem de erro inclui contagem atual e o limite.

Violações retornam `HTTP 422` com `code` específico (`invalid-status-transition`, `critical-task-admin-only`, `wip-limit-exceeded`, `assignee-not-member`).

---

## Arquitetura

```
com.taskmanager
├── api/
│   ├── controller/      # REST endpoints (parsing/binding)
│   ├── dto/             # request/response records
│   └── exception/       # GlobalExceptionHandler + exceções de domínio
├── application/         # services (regras de negócio + transações)
├── config/              # SecurityConfig, OpenApiConfig
├── domain/
│   ├── user/            # User, Role, UserRepository
│   ├── project/         # Project, ProjectRepository
│   └── task/            # Task, Status, Priority, TaskRepository, TaskSpecifications
└── security/            # JwtService, JwtAuthFilter, AppUserDetails, AuthenticatedUser
```

**Camadas**

- **Controller** — só faz binding de request/response e delega.
- **Service** — orquestra regras, transações e autorização.
- **Repository (JPA)** — Specifications dinâmicas para filtros, projection interfaces para o relatório agregado.
- **Domain** — entidades JPA + enums com lógica simples (`Status.canTransitionTo`).

---

## Decisões técnicas e tradeoffs

### PostgreSQL (prod) + H2 PostgreSQL-mode (test)
Banco real em produção, in-memory nos testes. Mesmo dialeto, mesmas migrations Flyway rodando em ambos. **Tradeoff**: o índice funcional `LOWER(title)` que eu colocaria em produção foi removido porque H2 não suporta — no Postgres real adicionaria via migration `V4__pg_only_indexes.sql` com `vendor=postgresql`, ou um índice GIN trigram (`pg_trgm`) para busca textual em escala maior.

### JWT stateless
Sem refresh token e sem blacklist. Decisão pragmática para o escopo do desafio. **Em produção**: refresh token com rotação + revogação por `jti` em Redis.

### Sem `BaseController`/`BaseService` genéricos
Considerei abstrair CRUD genérico, mas optei por **não fazer**. Razões:
- São apenas 3 entidades, todas com comportamentos divergentes (auth, ownership, regras específicas).
- O critério "código limpo sem over-engineering" no PDF do desafio aponta nessa direção.
- A consistência vem da **disciplina** nos nomes (`create/list/get/update/delete`), não de herança.

### JPA Specifications para filtros
`TaskSpecifications.withFilters(...)` constrói predicates dinâmicos em uma única query parametrizada — ao invés de N endpoints ou string concatenation. Boa em legibilidade e seguro contra SQL injection.

### Autorização por checagem manual no service
Optei por checar membership/ownership dentro dos services (com helper `AuthenticatedUser`) ao invés de `@PreAuthorize` SpEL. **Razão**: as regras envolvem o domínio (`project.hasMember(userId)`), não só roles — fica mais legível e testável em Java do que em expressões SpEL.

### `ProblemDetail` (RFC 7807) padronizado
`GlobalExceptionHandler` retorna `ProblemDetail` com `type`, `title`, `detail`, `code`, `timestamp` e `errors` (mapa de campo → mensagem para validações). Status codes consistentes em toda a API.

### Mensagens em português
- **Bean Validation** → `message=` colado na anotação do DTO. Tradeoff: se houvesse i18n real, eu usaria `ValidationMessages_pt_BR.properties`. Para um app monolíngue, a clareza no DTO ganha da indireção.
- **Regras de negócio** → mensagens nos services. Identificadores de domínio (`CRITICAL`, `IN_PROGRESS`, `ADMIN`...) ficam em inglês porque são valores de contrato (JSON, banco, frontend).

### Segurança no register público
`POST /v1/auth/register` ignora qualquer campo `roles` no body e cria o usuário **sempre como MEMBER**. Para criar outro `ADMIN` é preciso estar logado como `ADMIN` e usar `POST /v1/auth/admins`. Isso evita escalada de privilégio trivial (qualquer um se registrando como admin).

### Race condition no WIP limit
A checagem do limite de 5 IN_PROGRESS por responsável usa `@Lock(PESSIMISTIC_READ)` na query de contagem ([`TaskRepository.countInProgressForUpdate`](backend/src/main/java/com/taskmanager/domain/task/TaskRepository.java)). Sem o lock, dois PUTs concorrentes para o mesmo assignee poderiam cada um ler `count=4` e ambos passarem.

### Page size com teto
`spring.data.web.pageable.max-page-size: 100` no `application.yml` + `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)` no `TaskController.list`. Cliente não consegue mandar `?size=99999` e travar a query.

### `findByIdWithRelations` evita lazy loading
[`TaskRepository.findByIdWithRelations`](backend/src/main/java/com/taskmanager/domain/task/TaskRepository.java) faz `JOIN FETCH` no `assignee` e no `project`. Sem isso, a serialização de `TaskResponse` dispararia lazy load por linha (ainda mais grave com `open-in-view: false`).

### `isAccessibleBy` como query enxuta
Para listagem e relatório, [`ProjectRepository.isAccessibleBy`](backend/src/main/java/com/taskmanager/domain/project/ProjectRepository.java) faz `SELECT CASE WHEN COUNT > 0` em uma única query, sem carregar a coleção `members` para a memória. Mantém `findByIdWithMembers` apenas onde realmente precisamos da entidade carregada (create/update task).

### Filter retorna 401 em token inválido/expirado
O `JwtAuthenticationFilter` agora escreve um `ProblemDetail` 401 quando o token falha na validação, ao invés de deixar a request seguir e cair em 403 genérico.

### `open-in-view: false`
Lazy loading consciente — sem N+1 escondido renderizando JSON. Onde precisei das coleções, uso `JOIN FETCH` explícito (ver `ProjectRepository.findByIdWithMembers`).

### Frontend: Context API ao invés de Redux/Zustand
A única coisa global é o usuário autenticado e o token JWT. Estado de listas (projetos, tarefas) é local em cada página, recarregado sob demanda. Redux/Zustand seria over-engineering aqui. Justificativa: o ganho de uma store global só compensa quando há estado compartilhado entre rotas distantes — não é o caso.

---

## Estratégia de testes

| Tipo            | Local                                   | Cobre                                                                                                   |
|-----------------|-----------------------------------------|---------------------------------------------------------------------------------------------------------|
| Unitário (Mockito) | `application/TaskServiceTest`        | 11 testes nas **3 regras de negócio** + edge cases (assignee não-membro, transições válidas/inválidas)  |
| Unitário (Mockito) | `application/ProjectServiceTest`     | 12 testes — create/update/delete + addMember/removeMember + autorização (owner / ADMIN / outsider)      |
| Integração (`@SpringBootTest` + MockMvc) | `integration/TaskFlowIntegrationTest` | Fluxo crítico end-to-end: seed admin → login → criar projeto → adicionar membro → criar/iniciar tarefa → listar com filtro → buscar por texto → relatório → delete |
| Componente (Vitest + Testing Library) | `frontend/.../PriorityBadge.test.tsx` | Render e estilo do badge por prioridade                                                                 |

**Por que essa divisão:**
- Unitário cobre onde está a complexidade de domínio (regras de negócio).
- Integração valida toda a stack (Security + JPA + JSON + ProblemDetail) sem mocks — pega regressão de wiring.
- Não persegui 100% de cobertura: testei o que tem regra de negócio e os caminhos de erro mais relevantes.

```bash
# Backend
cd backend && mvn test

# Frontend
cd frontend && npm test
```

---

## Estrutura de pastas

```
task-manager/
├── README.md
├── docker-compose.yml      # PostgreSQL 16
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/taskmanager/
│       │   │   ├── api/{controller,dto,exception}
│       │   │   ├── application/      # services
│       │   │   ├── config/
│       │   │   ├── domain/{user,project,task}
│       │   │   └── security/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/     # V1, V2, V3
│       └── test/
│           ├── java/com/taskmanager/
│           │   ├── application/      # TaskServiceTest (11 testes)
│           │   └── integration/      # TaskFlowIntegrationTest (3 testes)
│           └── resources/
│               └── application.yml   # perfil 'test' com H2
└── frontend/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/                       # client + endpoints
        ├── components/                # PriorityBadge, StatusBadge (+ teste)
        ├── pages/                     # Login, Projects, ProjectDetail
        ├── store/                     # AuthContext
        └── types/                     # Status, Priority, Role + ROLE_LABEL
```
