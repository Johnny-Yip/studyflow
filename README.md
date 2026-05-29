# Study Flow Sync
[![CI](https://github.com/Johnny-Yip/studyflow/actions/workflows/ci.yml/badge.svg)](https://github.com/Johnny-Yip/studyflow/actions/workflows/ci.yml)

Study Flow Sync is a full-stack student planner built with Java 17, Spring Boot, and a static HTML/CSS/JavaScript frontend. It keeps the original StudyFlow course, task, and grade dashboard, then adds a Canvas-powered smart study planner that syncs Canvas courses and assignments into a local SQLite database.

## Features

- User registration and login APIs
- BCrypt password hashing
- JWT authentication for API access
- Protected course, task, grade, and dashboard endpoints
- Per-user data isolation (users only access their own records)
- Course management with create, read, update, and delete support
- Task management with status tracking (`TODO`, `IN_PROGRESS`, `DONE`)
- Task search/filter/sort by title, status, priority, course, and sort mode
- Grade management with weighted score calculations
- Dashboard summary statistics (courses, tasks, completion, overdue)
- Canvas connection settings screen with base URL, access token input, and Test Connection
- Canvas sync for courses, assignments, To-do items, and Planner items when supported
- Local SQLite storage for Canvas `courses`, `assignments`, `tasks`, and `sync_logs`
- Smarter Canvas task buckets: Today, This Week, Overdue, No Due Date, and High Priority
- Priority score from 0 to 100 based on due date, overdue/completed state, and missing status
- Missing assignment detector that shows assignments even when Canvas To-do omits them
- Source labels for Assignment API, Todo API, and Planner API
- Mock Canvas sync data for demos when Canvas is unavailable
- Frontend login/register flow and authenticated dashboard UI
- H2 in-memory database for local development
- JUnit + Mockito unit tests and integration tests for auth/security
- GitHub Actions CI (`mvn test`)

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Spring Security
- Java 11+ `HttpClient`
- Jackson JSON parsing
- SQLite + JDBC (`sqlite-jdbc`)
- JWT (`jjwt`)
- Bean Validation
- H2 Database
- Maven
- JUnit 5
- Mockito
- HTML
- CSS
- JavaScript

## Screenshots

### Dashboard

![StudyFlow Dashboard](docs/screenshots/dashboard.png)

### Courses

![Courses Section](docs/screenshots/courses.png)

### Tasks

![Tasks Section](docs/screenshots/tasks.png)

### Grades

![Grades Section](docs/screenshots/grades.png)

## Setup Instructions

### Prerequisites

- Java 17 or newer
- Maven 3.9 or newer
- Git

### Clone the Repository

```bash
git clone <your-repository-url>
cd StudyFlow
```

### Run the Application

```bash
mvn spring-boot:run
```

Application URL:

```text
http://localhost:8080
```

If port `8080` is already in use, stop the other local app first and rerun the same command. For a one-off alternate port during local troubleshooting, you can use:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Authentication

StudyFlow uses stateless JWT authentication.

For local development, StudyFlow generates an in-memory JWT signing key if `STUDYFLOW_JWT_SECRET` is not set. If you want existing JWTs to stay valid across app restarts, set a local environment variable before starting the app:

```bash
export STUDYFLOW_JWT_SECRET="$(openssl rand -base64 32)"
mvn spring-boot:run
```

Do not commit real JWT secrets, Canvas tokens, `.env` files, local config files, or local database files.

### Demo Account (Seeded)

When the app starts with an empty database, one demo account is created:

```text
name: Demo Student
email: student@example.com
password: password123
```

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Student",
    "email": "alice@example.com",
    "password": "password123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "password123"
  }'
```

Example auth response:

```json
{
  "token": "<jwt-token>",
  "tokenType": "Bearer",
  "userId": 2,
  "name": "Alice Student",
  "email": "alice@example.com"
}
```

Use the token for protected endpoints:

```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <jwt-token>"
```

## H2 Database Console

The H2 console is available at:

```text
http://localhost:8080/h2-console
```

Use these settings:

```text
JDBC URL: jdbc:h2:mem:studyflow
Username: sa
Password:
```

## Frontend Usage

Open:

```text
http://localhost:8080/
```

Flow:

1. Register a new account (or sign in with the seeded demo account).
2. The app stores the JWT in browser storage.
3. All dashboard operations call protected APIs with `Authorization: Bearer <token>`.
4. Use the top navigation tabs for Courses, Tasks, Canvas Sync, and Grades.
5. Open the Canvas Sync tab to test a Canvas connection, sync real Canvas data, or load mock demo data.

## Canvas Setup

### Open the Canvas Sync Page

1. Start the app with `mvn spring-boot:run`.
2. Open `http://localhost:8080/`.
3. Sign in or register.
4. Click the `Canvas Sync` tab in the authenticated dashboard header.

The Canvas Sync page contains the Canvas base URL field, access token field, Test connection button, Sync Canvas button, Load demo Canvas data button, task bucket filters, and recent local sync logs.

### Create a Canvas Access Token

1. Log in to your Canvas instance, for example `https://school.instructure.com`.
2. Open Account, then Settings.
3. Under Approved Integrations, choose New Access Token.
4. Add a purpose such as `Study Flow Sync`.
5. Choose an expiration date if your school requires one.
6. Generate the token and copy it immediately.

If your school disables personal access tokens, ask your Canvas administrator whether API tokens are available for students.

### Connect From The App

In the Canvas Sync tab:

1. Enter the Canvas base URL, for example `https://school.instructure.com`.
2. Paste the access token.
3. Click Test connection.
4. Click Sync Canvas.

The token is not saved by the backend. The browser keeps it only in `sessionStorage` for the current browser session.

### Run Mock Sync Without A Canvas Token

Use mock sync when you want to demo the Canvas dashboard before getting a real token:

1. Start the app and sign in.
2. Open the `Canvas Sync` tab.
3. Leave the Canvas base URL and access token blank.
4. Click `Load demo Canvas data`.
5. Confirm the task list shows demo Canvas assignments, including High Priority, Overdue, and No Due Date examples.

Mock sync uses built-in sample data only. It does not call Canvas.

### Test With A Real Canvas Account

1. Start the app and sign in.
2. Open the `Canvas Sync` tab.
3. Enter your Canvas base URL, such as `https://school.instructure.com`.
4. Paste your personal access token into the Access token field.
5. Click `Test connection` and confirm the success message reports active courses.
6. Click `Sync Canvas`.
7. Review the Canvas task buckets: `Today`, `This Week`, `Overdue`, `No Due Date`, and `High Priority`.
8. Check source badges on tasks. Assignments found through the Assignments API still appear even if Canvas To-do did not list them.

### Environment Variable Option

You can avoid typing the Canvas settings into the UI by setting local environment variables before starting the app:

```bash
export STUDYFLOW_CANVAS_BASE_URL="https://school.instructure.com"
export STUDYFLOW_CANVAS_TOKEN="<canvas-token>"
mvn spring-boot:run
```

### Local Storage And Security

- Canvas data is stored locally in `data/canvas-sync.db`.
- `data/`, `.env` files, local SQLite/database files, log files, and local Canvas config file names are ignored by Git.
- The Canvas token is never hardcoded, stored in SQLite, or printed in application logs.
- The backend supports Canvas pagination through `Link` headers.
- Invalid URLs, invalid tokens, network failures, rate limits, empty courses, and assignments with no due date are handled with safe API errors or warnings.

## API Endpoint Examples

All endpoints below (except `/api/auth/**`) require `Authorization: Bearer <jwt-token>`.

### Dashboard

```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <jwt-token>"
```

### Courses

Create a course:

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Algorithms",
    "description": "Graph algorithms and dynamic programming"
  }'
```

Get current user's courses:

```bash
curl http://localhost:8080/api/courses \
  -H "Authorization: Bearer <jwt-token>"
```

Get one course:

```bash
curl http://localhost:8080/api/courses/1 \
  -H "Authorization: Bearer <jwt-token>"
```

Update a course:

```bash
curl -X PUT http://localhost:8080/api/courses/1 \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Advanced Algorithms",
    "description": "Greedy, graphs, and DP"
  }'
```

Delete a course:

```bash
curl -X DELETE http://localhost:8080/api/courses/1 \
  -H "Authorization: Bearer <jwt-token>"
```

### Tasks

Create a task for a course:

```bash
curl -X POST http://localhost:8080/api/courses/1/tasks \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finish homework 1",
    "description": "Complete exercises 1 through 10",
    "dueDate": "2026-06-01",
    "priority": "HIGH",
    "status": "TODO"
  }'
```

Get tasks for one course:

```bash
curl http://localhost:8080/api/courses/1/tasks \
  -H "Authorization: Bearer <jwt-token>"
```

Search/filter/sort tasks:

```bash
curl "http://localhost:8080/api/tasks?title=homework&status=TODO&priority=HIGH&courseId=1&sort=dueDate" \
  -H "Authorization: Bearer <jwt-token>"
```

Sort tasks by priority:

```bash
curl "http://localhost:8080/api/tasks?sort=priority" \
  -H "Authorization: Bearer <jwt-token>"
```

Get one task:

```bash
curl http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <jwt-token>"
```

Update a task:

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finish homework 1",
    "description": "Complete and review exercises 1 through 10",
    "dueDate": "2026-06-03",
    "priority": "MEDIUM",
    "status": "IN_PROGRESS"
  }'
```

Delete a task:

```bash
curl -X DELETE http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <jwt-token>"
```

### Grades

Create a grade for a course:

```bash
curl -X POST http://localhost:8080/api/courses/1/grades \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "assignmentName": "Midterm exam",
    "score": 92,
    "maxScore": 100,
    "weight": 30
  }'
```

Get grades for a course:

```bash
curl http://localhost:8080/api/courses/1/grades \
  -H "Authorization: Bearer <jwt-token>"
```

Get one grade:

```bash
curl http://localhost:8080/api/grades/1 \
  -H "Authorization: Bearer <jwt-token>"
```

Update a grade:

```bash
curl -X PUT http://localhost:8080/api/grades/1 \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "assignmentName": "Midterm exam",
    "score": 95,
    "maxScore": 100,
    "weight": 30
  }'
```

Delete a grade:

```bash
curl -X DELETE http://localhost:8080/api/grades/1 \
  -H "Authorization: Bearer <jwt-token>"
```

### Canvas Sync

Test a Canvas connection:

```bash
curl -X POST http://localhost:8080/api/canvas/test \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "baseUrl": "https://school.instructure.com",
    "accessToken": "<canvas-token>"
  }'
```

Sync Canvas data:

```bash
curl -X POST http://localhost:8080/api/canvas/sync \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "baseUrl": "https://school.instructure.com",
    "accessToken": "<canvas-token>"
  }'
```

Load mock Canvas data without a token:

```bash
curl -X POST http://localhost:8080/api/canvas/mock-sync \
  -H "Authorization: Bearer <jwt-token>"
```

Get synced Canvas tasks:

```bash
curl "http://localhost:8080/api/canvas/tasks?bucket=HIGH_PRIORITY" \
  -H "Authorization: Bearer <jwt-token>"
```

Supported task buckets are `ALL`, `TODAY`, `THIS_WEEK`, `OVERDUE`, `NO_DUE_DATE`, and `HIGH_PRIORITY`.

Get recent Canvas sync logs:

```bash
curl http://localhost:8080/api/canvas/sync-logs \
  -H "Authorization: Bearer <jwt-token>"
```

## Validation and Error Responses

Example validation error response:

```json
{
  "timestamp": "2026-05-26T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "name": "Course name is required"
  }
}
```

Authentication errors return `401 Unauthorized`.
Missing resources return `404 Not Found`.
Duplicate registration email returns `409 Conflict`.
Canvas rate limits return `429 Too Many Requests` when Canvas sends that status.

## Tests

Run all tests:

```bash
mvn test
```

The suite includes:

- Service-layer unit tests for courses, tasks, grades, and dashboard
- Canvas priority algorithm tests
- Mock Canvas sync and SQLite storage tests
- Integration tests for registration/login
- Integration tests for protected endpoint access and data ownership isolation

## Project Structure

```text
StudyFlow
├── .github/workflows/ci.yml
├── src
│   ├── main
│   │   ├── java/com/studyflow
│   │   │   ├── canvas
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   ├── entity
│   │   │   ├── exception
│   │   │   ├── repository
│   │   │   ├── security
│   │   │   └── service
│   │   └── resources
│   │       ├── static
│   │       │   ├── app.js
│   │       │   ├── index.html
│   │       │   └── styles.css
│   │       └── application.properties
│   └── test
│       ├── java/com/studyflow
│       │   ├── canvas
│       │   ├── controller
│       │   └── service
│       └── resources/mockito-extensions
├── pom.xml
└── README.md
```

## Author

Created as a portfolio project to demonstrate practical backend development, RESTful API design, secure authentication, frontend integration, and maintainable test coverage.
