# Adventure Book API

> **_NOTE:_**  This project implements all 6 challenge objectives.

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
- The database is queried with jOOQ. Its table classes are generated on every build into `target/generated-sources/jooq`, by applying the same migrations to a temporary PostgreSQL container, so Docker must be running to compile. After changing a migration, rebuild to refresh them.
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

### Adding a book

Post a book in the same JSON format as the files in `src/main/resources/seed/books`:

```bash
curl -i -X POST -H 'Content-Type: application/json' -d @src/main/resources/seed/books/valid-crystal-caverns.json http://localhost:8080/books
```

The response is the new book (201), and its `Location` header points to it. A book that breaks the import rules is rejected (400) with the reason: it needs exactly one `BEGIN` section, at least one `END` section, unique section ids, and options that lead to existing sections. Books are not checked for duplicates, so posting the same book twice adds it twice.

### Playing a book

A player is identified by the `X-Username` header, which every adventure endpoint requires. A player can have several adventures at once, on the same book or on different books, and can only see their own.

Each book returned by `/books` has a `start` link. Posting the book's id to it starts an adventure at the book's first section with 10 health points:

```bash
curl -i -X POST -H 'X-Username: alice' -H 'Content-Type: application/json' -d '{"bookId": 1}' http://localhost:8080/adventures/start
```

The response is the adventure's state, and its `Location` header points to the new adventure. Read the state again at any time to resume:

```bash
curl -H 'X-Username: alice' http://localhost:8080/adventures/1
```

The state lists the options of the current section, each with its `option` number (starting at 0) and the consequences of taking it. Move by choosing one:

```bash
curl -X POST -H 'X-Username: alice' -H 'Content-Type: application/json' -d '{"option": 0}' http://localhost:8080/adventures/1/move
```

Consequences add or remove health. The adventure's `status` is `IN_PROGRESS`, `DEAD` when health reaches zero, or `FINISHED` on an ending section. A dead or finished adventure can no longer move (409); start a new one to play again.

List your adventures:

```bash
curl -H 'X-Username: alice' http://localhost:8080/adventures
```

Delete one:

```bash
curl -X DELETE -H 'X-Username: alice' http://localhost:8080/adventures/1
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

This generates the jOOQ classes and runs the tests, so Docker must be running, and creates the application JAR in `target/`.

## Run the built JAR

With the database started, replace `<version>` with the generated version:

```bash
java -jar target/adventure-book-api-<version>.jar --spring.profiles.active=local
```

## Windows

Replace `./mvnw` with `.\mvnw.cmd` in the commands above.
