# AGENTS.md - Codebase Context for AI AGENTS

**NOTE FOR FUTURE AI AGENTS:**
- Read this file FIRST before starting any new task
- Use this context to write accurate task prompts, execution plans, and milestones
- Update this file after your task completes (incremental updates only for relevant changes)

## Last Updated
- **Date**: 2026-11-25T00:35:00Z
- **Task**: Bugbot fixes + application.yml config
- **Developer**: AI Agent

## Description
Long-lived Spring Boot service that accepts book JSON + cover image, builds and EpubCheck-validates a single EPUB in memory, and exposes the latest validation report per book id. Persistence and volume splitting belong to callers (orchestrator).

## Technology Stack
- Java 25, Maven multi-module
- Spring Boot 4.2.0 (`spring-boot-starter-web`, Log4j2)
- FreeMarker EPUB templates, Jackson, EpubCheck 5.4.0
- Container: multi-stage Dockerfile (Maven Corretto 25 → Amazon Corretto 25)

## Project structure
```
epub-generator-build/          # root aggregator POM
  epub-generator-bom/          # dependency/plugin BOM (exec classifier fat JAR)
  epub-generator/              # Spring Boot app module
    src/main/java/.../web/     # REST controllers + exception advice
    src/main/java/.../generator/  # in-memory generate + report store
    src/main/resources/application.yml  # multipart limits + report-store cap
    src/main/resources/templates/ # FreeMarker EPUB templates
  scripts/                     # container smoke test
  .github/workflows/           # Docker Hub build/push + smoke
```

## Dependency Files
- Root `pom.xml`, `epub-generator-bom/pom.xml`, `epub-generator/pom.xml`
- `Dockerfile`

## Local Development Setup

**Prerequisites:** JDK 25+, Maven 3.9+, Podman (or Docker) for image smoke

**Environment Setup:** Defaults in `epub-generator/src/main/resources/application.yml` (`spring.servlet.multipart.max-file-size` / `max-request-size`, `epub.report-store.max-entries`). No `.env` required.

**Setup Commands:**
1. **Install / build:** `mvn --batch-mode --errors --fail-at-end --show-version clean install`
2. **Run tests:** included in `mvn clean install` (Surefire `*Test.java`)
3. **Start:** `java -jar epub-generator/target/epub-generator-exec.jar` (port 8080)
4. **Podman:** `podman build -t epub-generator .` then `podman run --rm -p 8080:8080 epub-generator`
5. **Smoke:** `bash scripts/smoke-container.sh epub-generator:smoke`

**Verification:**
- Unit/WebMvc tests under `epub-generator/src/test`
- `scripts/smoke-container.sh` hits `POST /api/v1/epub` and `GET /api/v1/epub/reports`
- API and JSON shapes: see `readme.md`

**Common Issues:**
- Use `epub-generator-exec.jar` (classifier `exec`), not the thin jar
- Cover part (JPEG/PNG) and non-blank `book.id` are required on generate
- Oversized uploads return HTTP 413; raise multipart limits in `application.yml` if needed

## Development workflow
- Default branch: `main`
- CI: `.github/workflows/docker-image.yml` — smoke build on PR/push, multi-arch push to Docker Hub on push to `main`
- Prefer small PRs; keep `AGENTS.md` aligned with structural/command changes
