# Cloud Task API

Small Spring Boot REST API that manages tasks and shows a complete local delivery path: Maven build, automated tests, OpenAPI, Actuator health, Docker, and GitHub Actions. The domain is intentionally small. The project is organized so the build, container, and deployment steps are easy to follow.

Nothing in this repository has been pushed to GitHub, and no cloud virtual machine has been provisioned.

## Project objectives

- Provide a JSON REST API for creating, reading, updating, deleting, and filtering tasks.
- Keep a layered Spring Boot 3 application on Java 21.
- Build and test it with Maven, including the Maven Wrapper.
- Run it in a Linux container as a non-root user.
- Check health with Spring Boot Actuator.
- Run tests and a Docker image build in GitHub Actions.
- Document how to deploy that image to an Ubuntu VM after a repository and a VM exist.

## Technology stack

| Technology | Role |
| --- | --- |
| Java 21 | Language and runtime |
| Spring Boot 3.5 | Application framework |
| Spring Web | REST controllers |
| Spring Data JPA and Hibernate | Persistence |
| H2 | Default in-memory database |
| PostgreSQL driver | Present so the optional prod profile can connect later |
| Jakarta Bean Validation | Request validation |
| springdoc OpenAPI | Swagger UI at `/swagger-ui/index.html` |
| Spring Boot Actuator | `/actuator/health` and `/actuator/info` |
| JUnit 5, Mockito, AssertJ, MockMvc | Unit and integration tests |
| Maven Wrapper | Repeatable builds without a preinstalled Maven version |
| Docker and Compose | Multi-stage image and local container run |
| GitHub Actions | CI on `main`, plus a separate manual deploy workflow |

Kubernetes, Kafka, Redis, and extra services are not used.

## Architecture

Requests enter a controller, which validates the JSON body and calls a service. The service owns the business rules and maps entities to response DTOs. Spring Data JPA repositories talk to H2. Controllers do not return JPA entities.

`GlobalExceptionHandler` turns validation failures, bad path values, missing tasks, and unexpected errors into one JSON shape. Unexpected exceptions are logged on the server. Clients receive a short message and no stack trace.

```text
HTTP client
    -> controller
        -> service
            -> repository
                -> H2 (in memory)
```

Packages under `com.shubham.cloudapi`:

| Package | Responsibility |
| --- | --- |
| `controller` | HTTP mapping and status codes |
| `service` | Task rules and DTO mapping |
| `repository` | Spring Data JPA queries |
| `entity` | `Task` and the status and priority enums |
| `dto` | Request, response, and error records |
| `exception` | Not-found exception and the global handler |
| `config` | OpenAPI metadata |

Dependencies are constructor-injected.

## Folder structure

```text
cloud-deployed-java-rest-api/
├── .github/workflows/
│   ├── ci-cd.yml
│   └── deploy.yml
├── docker/entrypoint.sh
├── docs/
│   ├── CICD.md
│   └── DEPLOYMENT.md
├── postman/Cloud-Task-API.postman_collection.json
├── scripts/deploy.sh
├── src/main/java/com/shubham/cloudapi/
│   ├── CloudApiApplication.java
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── exception/
│   ├── repository/
│   └── service/
├── src/main/resources/application.yml
├── src/test/java/com/shubham/cloudapi/
├── application-prod.yml.example
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## Prerequisites

- JDK 21. The wrapper still downloads Maven if `mvn` is not installed.
- Docker Engine with the Compose plugin, when you want to run the container.
- Git, when you want version control.
- curl, or a browser, to call the API.

Local development does not need PostgreSQL.

## Local setup

```bash
git clone <YOUR_REPOSITORY_URL>
cd cloud-deployed-java-rest-api
```

On Windows, either the Maven Wrapper scripts or an installed Maven can build the project:

```bat
mvnw.cmd verify
```

```bash
./mvnw verify
```

The default configuration starts with an empty in-memory H2 database named `taskdb`. Data lives only as long as that JVM process. The H2 console is disabled. `ddl-auto` is `update`, which creates the `tasks` table on startup.

`application-prod.yml.example` shows a PostgreSQL profile. Spring Boot does not load the example file. Copy it to `src/main/resources/application-prod.yml` or `./config/application-prod.yml`, set `DATABASE_PASSWORD`, and start with `SPRING_PROFILES_ACTIVE=prod`. Do not commit the copied file or a real password.

## Maven

The wrapper is the command to use in CI and in Docker.

| Command | What it does |
| --- | --- |
| `./mvnw clean` | Deletes `target/` |
| `./mvnw compile` | Compiles main sources |
| `./mvnw test` | Runs unit and integration tests |
| `./mvnw package` | Builds `target/cloud-task-api.jar` |
| `./mvnw verify` | Runs tests and then packages |

On Windows, use `mvnw.cmd` instead of `./mvnw`. An installed Maven 3.9+ works the same way if you replace `./mvnw` with `mvn`.

## Running the application

```bash
./mvnw spring-boot:run
```

```bat
mvnw.cmd spring-boot:run
```

The API listens on port 8080 unless `SERVER_PORT` is set.

| URL | Purpose |
| --- | --- |
| http://localhost:8080/api/info | Name, description, and version |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI document |
| http://localhost:8080/actuator/health | Health, details hidden |
| http://localhost:8080/actuator/info | Build and application info |

Stop the process with Ctrl+C.

## API endpoints

All task bodies are JSON. Timestamps are ISO-8601 instants generated by Hibernate.

Task fields:

| Field | Rule |
| --- | --- |
| `id` | Generated number |
| `title` | Required, at most 120 characters |
| `description` | Optional, at most 2000 characters |
| `status` | `PENDING`, `IN_PROGRESS`, or `COMPLETED` |
| `priority` | `LOW`, `MEDIUM`, or `HIGH` |
| `createdAt` | Set on insert |
| `updatedAt` | Set on insert and update |

| Method and path | Success | Failure |
| --- | --- | --- |
| `POST /api/tasks` | 201 and `Location` | 400 validation or bad JSON |
| `GET /api/tasks` | 200 array, newest first | |
| `GET /api/tasks/{id}` | 200 | 400 bad id, 404 missing |
| `PUT /api/tasks/{id}` | 200 replaced task | 400 validation, 404 missing |
| `DELETE /api/tasks/{id}` | 204 empty body | 404 missing |
| `GET /api/tasks/status/{status}` | 200 matching array | 400 unknown status |
| `GET /api/info` | 200 metadata | |

Create example:

```bash
curl -sS -D - http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Write deployment notes\",\"description\":\"Document the Ubuntu VM rollout\",\"status\":\"PENDING\",\"priority\":\"HIGH\"}"
```

Error example for a missing task:

```json
{
  "timestamp": "2026-11-19T00:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id 42",
  "path": "/api/tasks/42"
}
```

Validation errors add `fieldErrors` with `field` and `message`. Responses do not include `trace` or a stack trace.

Swagger UI documents the same operations, schemas, and status codes.

## Postman

Import `postman/Cloud-Task-API.postman_collection.json`. The collection variable `baseUrl` defaults to `http://localhost:8080`. Send **Create Task** first. Its test script stores the new id in `taskId`, which the get, update, and delete requests use.

The collection also includes a validation request, a missing-task request, the status filter, `/api/info`, and `/actuator/health`.

## Docker

The Dockerfile is a multi-stage build. The first stage uses the Temurin 21 JDK and the Maven Wrapper to package the JAR. The runtime stage uses the Temurin 21 JRE, installs `curl` for the health check, and runs as the system user `app`. The JVM uses `MaxRAMPercentage=75.0` so the heap follows the container memory limit.

H2 stays in memory inside the container, so Compose does not mount a volume. The service joins the bridge network `task-api-net`, restarts unless stopped, and publishes host port 8080.

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f
docker compose down
```

`SERVER_PORT` in `.env` changes the published host port. The process inside the container still listens on 8080, which is the port the health check calls. Copy `.env.example` to `.env` when you want to override `JAVA_OPTS` or the host port. Leave `SPRING_PROFILES_ACTIVE` as `default` unless PostgreSQL is configured.

Confirm the running API:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/api/info
```

## GitHub Actions

`.github/workflows/ci-cd.yml` runs on pushes to `main` and on pull requests targeting `main`. It sets up Temurin 21, caches Maven, runs `./mvnw verify`, and then builds a local Docker image tagged with the commit SHA. It does not push the image.

`.github/workflows/deploy.yml` runs only when started manually. It needs the secrets `VM_HOST`, `VM_USER`, `VM_SSH_KEY`, and `VM_APP_DIR`. Those values are not in the repository.

Details are in [docs/CICD.md](docs/CICD.md).

## Cloud VM deployment

The Ubuntu steps, Docker Engine install, firewall rules, health checks, updates, logs, and troubleshooting live in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md). `scripts/deploy.sh` pulls when a git upstream exists, rebuilds the container, and waits for `/actuator/health`.

No VM has been created by this project.

## Environment variables

| Variable | Where | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | Process or Compose host mapping | Port. Inside Compose, the app port stays 8080 and this value selects the host port. |
| `SPRING_PROFILES_ACTIVE` | Process environment | `default` uses H2. `prod` uses the copied PostgreSQL file. |
| `JAVA_OPTS` | Container entrypoint | JVM flags. |
| `DATABASE_URL` | Prod profile | JDBC URL, defaulting to `jdbc:postgresql://localhost:5432/taskdb` in the example. |
| `DATABASE_USERNAME` | Prod profile | Database user, example default `taskapi`. |
| `DATABASE_PASSWORD` | Prod profile | Required when the prod profile is active. No default and no committed value. |

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Port 8080 is in use | Set `SERVER_PORT` to a free port and restart. |
| `./mvnw` is not executable | `chmod +x mvnw`, or use `mvnw.cmd` on Windows. |
| Tests fail on a dirty database | Tests use in-memory H2 and clear tasks before each integration test. Run `./mvnw clean verify`. |
| Docker daemon is not running | Start Docker and rerun `docker version` before Compose. |
| Container is unhealthy | `docker compose logs --tail=200` and curl `/actuator/health`. |
| Prod profile exits on startup | `DATABASE_PASSWORD` is missing or PostgreSQL is unreachable. Use the default profile to run with H2. |
| Swagger UI 404 | Open `/swagger-ui/index.html`. `/swagger-ui.html` redirects there. |

## Future improvements

- Add authentication before exposing the API beyond a locked-down network.
- Add pagination and filtering by priority.
- Replace `ddl-auto: update` with Flyway or Liquibase when a durable database is introduced.
- Publish the image to a registry and deploy that digest instead of building on the VM.
- Add a reverse proxy and TLS in front of port 8080.

## Database choice

The default database is in-memory H2. That keeps `docker compose up --build -d` self-contained and gives every test a clean database. It is the wrong place to store data you need after a restart. The PostgreSQL example is the path for a durable database, and it still refuses to embed a password in the project.
