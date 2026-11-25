# EPUB Generator

Spring Boot HTTP service that builds a single EPUB in memory from book JSON and a cover image, validates it with EpubCheck, and keeps the latest validation report per book id. No persistent book/EPUB storage.

## Build

Requires JDK 25+.

```bash
mvn --batch-mode --errors --fail-at-end --show-version clean install
```

Runnable fat JAR: `epub-generator/target/epub-generator-exec.jar`.

## Run (JAR)

```bash
java -jar epub-generator/target/epub-generator-exec.jar
```

Listens on port `8080`. Logs go to the console.

## Configuration

Defaults live in `epub-generator/src/main/resources/application.yml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB      # max size per multipart part (book JSON or cover)
      max-request-size: 55MB   # max size of the whole multipart request

epub:
  report-store:
    max-entries: 100           # in-memory EpubCheck reports; oldest evicted when exceeded
```

Override via Spring external config or env (e.g. `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE`, `EPUB_REPORT_STORE_MAX_ENTRIES`).

## Run (Podman)

```bash
podman build -t epub-generator .
podman run --rm -p 8080:8080 epub-generator
```

### Container smoke test

After building an image locally:

```bash
podman build -t epub-generator:smoke .
bash scripts/smoke-container.sh epub-generator:smoke
```

The script starts the container, calls both APIs with the sample book fixtures under `epub-generator/src/test/resources/generator/`, and tears the container down. CI runs the same smoke check before multi-arch push (Docker Hub).

## APIs

### `POST /api/v1/epub`

Generate one EPUB. Multipart form:

| Part | Content type | Description |
|------|--------------|-------------|
| `book` | `application/json` | Book document (see JSON structure below) |
| `cover` | `image/jpeg` or `image/png` | Cover image bytes |

**Success:** `200` with body `application/epub+zip` and `Content-Disposition: attachment; filename="<normalized-title>.epub"`. Also stores/replaces the EpubCheck report for `book.id`.

**Errors:** `400` for missing parts, invalid JSON, blank `id`, or unsupported cover type; `413` if upload exceeds configured max sizes; `500` for unexpected generation failures.

```bash
curl -sS -o out.epub -w "%{http_code}\n" \
  -F "book=@book.json;type=application/json" \
  -F "cover=@cover.jpg;type=image/jpeg" \
  http://localhost:8080/api/v1/epub
```

### `GET /api/v1/epub/reports`

| Query | Behavior |
|-------|----------|
| (none) | `200` JSON array of all in-memory reports |
| `bookId=<id>` | `200` single report JSON, or `404` if unknown |

```bash
curl -sS "http://localhost:8080/api/v1/epub/reports"
curl -sS "http://localhost:8080/api/v1/epub/reports?bookId=ancientgodlymonarch"
```

Reports are in-memory only (lost on restart). Only the latest report per book id is kept. The store is capped by `epub.report-store.max-entries` (oldest entries evicted).

## Book JSON structure

Request `book` part maps to `Book` / `Chapter`:

```json
{
  "id": "ancientgodlymonarch",
  "uid": "optional-uuid",
  "title": "Ancient Godly Monarch",
  "author": "Author Name",
  "translator": "Translator",
  "editor": "Editor",
  "publisher": "Publisher",
  "subject": "Fantasy",
  "description": "Blurb",
  "crawldate": "2024-01-15",
  "lastUpdated": "2024-01-15",
  "chapters": [
    {
      "id": "ch1",
      "chapterNumber": "1",
      "chapterTitle": "Chapter One",
      "body": "Chapter text with newlines.",
      "url": "https://example.com/chapter/1"
    }
  ]
}
```

| Field | Notes |
|-------|--------|
| `id` | Required for API; used as report key and cover entry name `{id}.jpg` or `{id}.png` |
| `title` | Used for EPUB filename (`title` lowercased, spaces removed) |
| `crawldate` / `lastUpdated` | `YYYY-MM-DD` |
| `chapters[].body` | Chapter text; XML-escaped in the EPUB (`?html`); newlines become paragraph breaks. Empty/null bodies are skipped |
| `chapters[].chapterNumber` / `chapterTitle` | JSON names for number and title |

Volume splitting is not performed here; callers should send one volume per request.

## Report JSON structure

```json
{
  "bookId": "ancientgodlymonarch",
  "epubName": "ancientgodlymonarch",
  "fatalErrorCount": 0,
  "errorCount": 0,
  "warningCount": 0,
  "messages": [
    "ERROR(RSC-005): ancientgodlymonarch/OEBPS/content.opf(1,1): example"
  ]
}
```

`messages` contains FATAL / ERROR / WARNING strings collected during EpubCheck.
