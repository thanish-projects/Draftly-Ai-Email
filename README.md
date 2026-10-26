# Email Drafting Agent

A Java 21 / Spring Boot 3 REST API that analyzes email with the configured OpenAI-compatible endpoint, drafts a professional reply, and persists the result in MySQL.

## Requirements

- Java 21
- Maven 3.9+
- Python 3.10+
- Docker Compose (or a MySQL 8.4 database)
- An API key for the configured AI endpoint

## Run locally

1. Set `OPENAI_API_KEY` in the environment or in the root `.env` file. The application imports `.env` as a properties file, and `.env` is excluded by `.gitignore`; do not commit API keys to source control.
2. Start MySQL: `docker compose up -d mysql`
3. Start the application: `mvn spring-boot:run`

The API listens on `http://localhost:8082`. MySQL defaults to `localhost:3306`, database `email_drafting_agent`, username `root`, and password `root` for the Docker Compose setup. Override these with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. The chat model can be changed with `OPENAI_CHAT_MODEL` (or `OPENAI_MODEL`), and the OpenAI-compatible API base URL with `OPENAI_BASE_URL`.

### Streamlit frontend

In a second terminal, install the frontend dependencies and start the UI:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r frontend\requirements.txt
streamlit run frontend\app.py
```

Open `http://localhost:8501`. The frontend connects to the Spring Boot API at `http://localhost:8082` by default; set `EMAIL_API_URL` to override it.

OpenAI-compatible API requests use configurable timeouts and bounded retries: `OPENAI_TIMEOUT_SECONDS` (default 30), `OPENAI_MAX_RETRIES` (default 2 retries after the first attempt), and `OPENAI_RETRY_DELAY_MILLIS` (default 250 ms exponential backoff). HTTP 429, transient 5xx responses, and request timeouts are retried; invalid responses and other HTTP errors are surfaced as provider errors.

## API

### Process, list, and retrieve email/task records

- `POST /api/email/process` accepts `subject` and `email`, then returns `intent`, `tasks`, `meetingSuggestions`, and `reply`.
- `GET /api/emails` returns paginated email history.
- `GET /api/emails/{id}` returns one saved email.
- `GET /api/tasks` returns saved extracted tasks, newest first.

Example process request:

```json
{
  "subject": "Budget Planning",
  "email": "Let's connect tomorrow afternoon and finalize budget proposal by Friday."
}
```

Interactive Swagger UI is available at `http://localhost:8080/swagger-ui.html`; the OpenAPI JSON document is at `http://localhost:8080/v3/api-docs`.

### Read and summarize an email

`POST /api/emails/read`

```json
{
  "subject": "Project Update",
  "email": "Let's connect tomorrow afternoon and finalize the budget proposal by Friday."
}
```

The response contains the AI-detected intent (`MEETING`, `TASK`, `BOTH`, or `GENERAL`) and a concise summary. The service cleans whitespace and common email signatures before asking the AI provider to identify important sentences, summarize, and classify the message.

Example response:

```json
{
  "intent": "BOTH",
  "summary": "Schedule meeting tomorrow afternoon and finalize budget proposal by Friday"
}
```

### Classify email intent

`POST /api/emails/classify`

```json
{
  "emailContent": "Let's connect tomorrow and share the report by Friday."
}
```

Returns one of `MEETING`, `TASK`, `BOTH`, or `GENERAL` with a confidence score from 0 to 100. The AI prompt distinguishes actual requests from mere mentions and asks for JSON only. This classifier is also used as the authoritative intent source in the saved email processing flow.

### Extract and save tasks

`POST /api/emails/tasks/extract`

```json
{
  "emailContent": "Please finalize the proposal by Friday. It is urgent."
}
```

Returns extracted tasks with a title, owner, ISO date, and priority, and saves them to the PostgreSQL `email_tasks` table. Relative dates are resolved by the AI against the current UTC date. The service assigns `HIGH` to urgent/immediate or today/tomorrow tasks, `MEDIUM` to tasks due this week, and `LOW` otherwise; an unspecified owner defaults to `Current User`. Tasks extracted during `/api/emails/process` are also saved and associated with their email history entry.

### Suggest meeting times

`POST /api/emails/schedule`

```json
{
  "emailContent": "Let's connect tomorrow afternoon."
}
```

The scheduler asks the AI to match any requested meeting time against the mocked availability, then returns three suggestions. Without a time preference, it uses the configured availability order.

```json
{
  "suggestions": [
    "Tomorrow 2:00 PM",
    "Tomorrow 3:00 PM",
    "Tomorrow 4:00 PM"
  ]
}
```

### Generate a professional reply

`POST /api/emails/reply/generate`

```json
{
  "intent": "BOTH",
  "tasks": [
    {
      "title": "Finalize budget proposal",
      "owner": "Current User",
      "dueDate": "2026-10-16",
      "priority": "HIGH"
    }
  ],
  "meetingSuggestions": [
    "Tomorrow 2:00 PM",
    "Tomorrow 3:00 PM",
    "Tomorrow 4:00 PM"
  ]
}
```

Returns a concise, professional `reply` based only on the supplied intent, tasks, and available meeting suggestions. The `/api/emails/process` flow generates and saves its reply using the extracted task list and, for meeting intents, the scheduling suggestions.

### Supervise generated output

`POST /api/emails/supervise`

Accepts the source `emailContent`, `intent`, extracted `tasks`, `meetingSuggestions`, and generated `reply`. It returns `APPROVED` with an empty `issues` list or `REJECTED` with validation issues. The email-processing flow supervises output before saving it; a rejected result is returned with HTTP 422 and is not persisted.

### Process an email

`POST /api/emails/process`

```json
{
  "emailContent": "Hi Alex, can we meet next week to review the launch plan? Please send the updated slides by Friday.",
  "sender": "Jordan Lee <jordan@example.com>",
  "subject": "Launch plan review"
}
```

`sender` and `subject` are optional hints. Processing runs the reader, intent classifier, applicable task extraction and scheduling stages, reply generation, and supervisor in sequence. Only approved results are persisted to `email_history` and, when applicable, `email_tasks`.

The response contains the persisted `emailId`, detected `intent`, extracted `tasks`, `meetingSuggestions`, generated `reply`, and supervision `status`.

```json
{
  "emailId": 1,
  "intent": "BOTH",
  "tasks": [
    {
      "title": "Finalize updated slides",
      "owner": "Current User",
      "dueDate": "2026-10-16",
      "priority": "HIGH"
    }
  ],
  "meetingSuggestions": [
    "Next Week Monday 10:00 AM",
    "Next Week Tuesday 2:00 PM",
    "Next Week Wednesday 11:00 AM"
  ],
  "reply": "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant",
  "status": "APPROVED"
}
```

### Retrieve processed emails

- `GET /api/emails/{id}` retrieves one saved result.
- `GET /api/emails?page=0&size=20` lists results, newest first by default. Standard Spring Data pagination and sorting parameters are supported.

Validation errors return HTTP 400, missing history entries return HTTP 404, and AI-provider errors return HTTP 502.
