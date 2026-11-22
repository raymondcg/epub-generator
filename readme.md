# EPUB Generator

Standalone Spring Boot CLI that reads book JSON files from disk and generates EPUB files.

## Setup

1. Copy the example env file and adjust paths if needed:

```bash
cp .env.example .env
```

```
BOOK_DB=
EPUB_OUTPUT=
CHAPTERS_PER_VOLUME=1000
```

2. Place each book as `{BOOK_DB}/{id}.json` with a matching cover `{BOOK_DB}/{id}.jpg`.

## Build

```bash
mvn --batch-mode --errors --fail-at-end --show-version clean install
```

## Run

```bash
mvn -pl epub-generator spring-boot:run
```

Or after build:

```bash
java -jar epub-generator/target/epub-generator-1.0-SNAPSHOT-exec.jar
```

The app loads `.env`, reads every `*.json` under `BOOK_DB`, and writes EPUB files to `EPUB_OUTPUT`.
