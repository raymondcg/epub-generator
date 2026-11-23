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

Optional JSON field `lastUpdated` (`YYYY-MM-DD`): if present and on or before an existing EPUB’s last-modified date, that volume is skipped (EpubCheck still runs). If omitted, it defaults to today. Recreate only when `lastUpdated` is after the EPUB’s last-modified date.

## Build

Requires JDK 25+.

```bash
mvn --batch-mode --errors --fail-at-end --show-version clean install
```

## Run

```bash
mkdir -p log
java -jar epub-generator/target/epub-generator.jar
```

The app loads `.env`, reads every `*.json` under `BOOK_DB`, and writes EPUB files to `EPUB_OUTPUT`. Application logs go to `log/run.log` (replaced each run). After all volumes finish, it writes a consolidated EpubCheck summary (fatal/error/warning totals and a reprint of every issue).
