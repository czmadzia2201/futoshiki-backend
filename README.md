# Futoshiki

## Overview

A full-stack implementation of the Futoshiki logic puzzle built with **Spring Boot** and **Angular**.

The application supports multiple puzzle sources, including a custom puzzle generator, external APIs and AI-generated boards.

The project was created to explore different approaches to puzzle generation, compare multiple puzzle providers and experiment with AI used at runtime as one of the available data sources.

Backend and frontend are maintained as separate projects. The frontend repository is available here:

**Frontend:** https://github.com/czmadzia2201/futoshiki-frontend

---

## Technologies

### Backend

- Java
- Spring Boot
- Maven
- REST API
- JUnit 5
- Mockito
- MockWebServer

### Frontend

- Angular
- TypeScript
- HTML / CSS

---

## Multiple Puzzle Providers

The application supports four independent puzzle providers that can be selected directly from the user interface.

The backend uses a provider-based architecture (Factory + Strategy patterns), exposing a single REST API to the frontend.

### Generator

The generator was developed specifically for this project together with a solver used to verify generated puzzles. Boards are generated locally without relying on external services.

### Files

Loads puzzles generated in advance and stored locally as JSON files.

### OpenAI

Uses the OpenAI API to generate puzzles at runtime.

This provider was implemented as an experiment demonstrating how AI can be integrated into an application behind a common provider interface. Due to response time and API cost, it is intended as a demonstration rather than the default gameplay option.

### Futoshiki.com

Downloads puzzles from the public Futoshiki.com API and converts them into the application's internal board model.

---

## Gameplay

- Board sizes from **4×4** to **9×9**
- Multiple difficulty levels
- Pencil mode
- Undo
- Automatic solution verification
- User preferences stored in Local Storage

---

## Project Goals

The project was created to:

- build a complete full-stack application using Spring Boot and Angular,
- implement a custom puzzle generator and solver,
- compare multiple independent puzzle providers,
- experiment with AI used as one of the runtime data sources,
- explore provider-based application architecture.

---

## Running the Project Locally

### Backend

```bash
mvn spring-boot:run
```

### Frontend

```bash
npm install
npm start
```

### Swagger
http://localhost:8080/shape-puzzles/swagger-ui/index.html


### OpenAI configuration

To enable the **OpenAI** provider, configure your API key in the backend application configuration before starting the project.

---

## Development

This project was built with some assistance from AI tools, primarily ChatGPT, which served as a discussion partner and guide for frontend development.

The overall architecture, design decisions, implementation, code reviews and final solutions were designed, evaluated and refined by the author.

---

## Future Improvements

- Further tune the generator to achieve more consistent difficulty levels.
- Publish the application online. The production version will most likely expose only the **Generator** provider, while the remaining providers will still be available locally and in the source code.

