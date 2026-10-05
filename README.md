# Adventure Book API

REST API for choose-your-own-adventure books, built with Java 25, Spring Boot and PostgreSQL.

## Requirements

- JDK 25
- Docker, for the local PostgreSQL database and for the tests
- Internet access for the first build to download Maven, dependencies and the `postgres:18` image

Maven is provided through the included Maven Wrapper.

## Run locally

All commands are run from the project root.

### 1. Start the database

```bash
docker compose up -d
```

This starts PostgreSQL 18 on `localhost:5432` with the database `adventure_book`. The data is kept in a Docker volume, so it survives restarts.

### 2. Start the API

The first time, start with the `local` and `seed` profiles to load the sample books:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,seed
```

After that, start with the `local` profile only:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application starts at `http://localhost:8080`. Press `Ctrl+C` to stop it.

Things to know:

- The `local` profile holds the database connection. Without it the application has no datasource and fails to start.
- The database schema is created and updated by Flyway on startup, from `src/main/resources/db/migration`.
- The `seed` profile imports every JSON file in `src/main/resources/seed/books`. It is meant to run once: running it again inserts the same books a second time.

### 3. Stop the database

```bash
docker compose down
```

To also delete the data and start from an empty database next time:

```bash
docker compose down -v
```

## Using the API

The endpoints are documented in Swagger UI, open in your browser: http://localhost:8080/swagger-ui/index.html

The OpenAPI document:

```bash
curl http://localhost:8080/api-docs
```

### Health check

```bash
curl http://localhost:8080/actuator/health
```

The response has `"status":"UP"` when the application and its database connection are healthy.

## Run tests

Docker must be running. The tests start their own temporary PostgreSQL container, so they do not need or touch the local database.

```bash
./mvnw test
```

## Build

```bash
./mvnw clean package
```

This runs the tests, so Docker must be running, and creates the application JAR in `target/`.

## Run the built JAR

With the database started, replace `<version>` with the generated version:

```bash
java -jar target/adventure-book-api-<version>.jar --spring.profiles.active=local
```

## Windows

Replace `./mvnw` with `.\mvnw.cmd` in the commands above.
