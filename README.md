# Adventure Book API

REST API built with Java 25 and Spring Boot.

## Requirements

- JDK 25
- Internet access for the first build to download Maven and dependencies

Maven is provided through the included Maven Wrapper.

## Build

From the project root:

```bash
./mvnw clean package
```

This runs the tests and creates the application JAR in `target/`.

## Run locally

```bash
./mvnw spring-boot:run
```

The application starts at `http://localhost:8080`.

### Health check

```bash
curl http://localhost:8080/actuator/health
```

Expected response when healthy:

```json
{"status":"UP"}
```

### API documentation

Swagger UI, open in your browser: http://localhost:8080/swagger-ui/index.html

#### API docs
```bash
curl http://localhost:8080/api-docs
```



---
Press `Ctrl+C` to stop the application.

## Run the built JAR

After building, run the JAR from `target/`, replacing `<version>` with the generated version:

```bash
java -jar target/adventure-book-api-<version>.jar
```

## Run tests

```bash
./mvnw test
```

## Windows

Replace `./mvnw` with `.\mvnw.cmd` in the commands above.