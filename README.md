# Todo API

A small Java 17 and Spring Boot REST API. Todos are kept in memory, so they reset when the application stops.

## Requirements

- Java 17
- Maven 3.6+

## Run

```sh
mvn spring-boot:run
```

The server listens on `http://localhost:8080`.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/todos` | Return all todos |
| `POST` | `/todos` | Create a todo from `{"title":"..."}` |
| `GET` | `/health` | Return `{"status":"UP"}` |

Example:

```sh
curl -X POST http://localhost:8080/todos -H "Content-Type: application/json" -d '{"title":"Learn Spring Boot"}'
curl http://localhost:8080/todos
curl http://localhost:8080/health
```

Blank todo titles are rejected with HTTP 400. A successful create returns HTTP 201 and a todo with an automatically assigned ID and `completed: false`.

## Verify and package

```sh
mvn test
mvn clean package
```

The tests cover health, creating/listing todos, and rejecting a blank title.
