# StudyFlow

StudyFlow is a Java 17 Spring Boot application for student study planning and task management. It uses Maven, Spring Data JPA, validation, and an in-memory H2 database.

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Maven
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database
- JUnit 5 and Mockito

## Project Structure

```text
src/main/java/com/studyflow
├── controller
├── dto
├── entity
├── exception
├── repository
└── service
```

## Run Locally

```bash
mvn spring-boot:run
```

The API starts at:

```text
http://localhost:8080
```

H2 console:

```text
http://localhost:8080/h2-console
```

H2 settings:

```text
JDBC URL: jdbc:h2:mem:studyflow
Username: sa
Password:
```

The app seeds one demo user:

```text
id: 1
name: Demo Student
email: student@example.com
```

## Run Tests

```bash
mvn test
```

## API Examples

### Create a Course

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Algorithms",
    "description": "Study graph algorithms and dynamic programming",
    "userId": 1
  }'
```

### Get All Courses

```bash
curl http://localhost:8080/api/courses
```

### Get Courses for a User

```bash
curl "http://localhost:8080/api/courses?userId=1"
```

### Get One Course

```bash
curl http://localhost:8080/api/courses/1
```

### Update a Course

```bash
curl -X PUT http://localhost:8080/api/courses/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Advanced Algorithms",
    "description": "Greedy algorithms, graphs, and dynamic programming"
  }'
```

### Delete a Course

```bash
curl -X DELETE http://localhost:8080/api/courses/1
```

### Create a Task for a Course

```bash
curl -X POST http://localhost:8080/api/courses/1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finish homework 1",
    "description": "Complete exercises 1 through 10",
    "dueDate": "2026-06-01",
    "priority": "HIGH",
    "status": "TODO"
  }'
```

Valid priorities:

```text
LOW, MEDIUM, HIGH
```

Valid task statuses:

```text
TODO, IN_PROGRESS, DONE
```

### Get Tasks for a Course

```bash
curl http://localhost:8080/api/courses/1/tasks
```

### Get One Task

```bash
curl http://localhost:8080/api/tasks/1
```

### Update a Task

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finish homework 1",
    "description": "Complete and review exercises 1 through 10",
    "dueDate": "2026-06-03",
    "priority": "MEDIUM",
    "status": "IN_PROGRESS"
  }'
```

### Delete a Task

```bash
curl -X DELETE http://localhost:8080/api/tasks/1
```

## Validation and Errors

Invalid requests return a structured error response. Example:

```json
{
  "timestamp": "2026-05-23T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "name": "Course name is required"
  }
}
```

Missing resources return `404 Not Found`.
