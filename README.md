# AuraLLM

This project is a Spring Boot application that provides a REST API for interacting with the Ollama service.

## Prerequisites

- Java 17 or higher
- Maven 3.2 or higher

## How to Build

To build the executable JAR, run the following command from the project's root directory:

```bash
mvn clean install
```

This will create a file named `AuraLLM-1.0-SNAPSHOT.jar` in the `target` directory.

## How to Run

To run the application, use the following command:

```bash
java -jar target/AuraLLM-1.0-SNAPSHOT.jar
```

## Configuration

The application requires the following properties to be configured in an `application.properties` file placed in the same directory as the JAR file:

- `ollama.api.url`: The URL of the Ollama API.
- `ollama.model`: The name of the Ollama model to use.
- `server.port`: The port on which the application will run.

Example `application.properties` file:

```properties
ollama.api.url=http://121.0.0.1:11434/api/generate
ollama.model=mistral
server.port=1025
```
