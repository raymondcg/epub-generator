#!/usr/bin/env bash
set -euo pipefail

IMAGE_TAG="${1:-epub-generator:smoke}"
CONTAINER_NAME="epub-generator-smoke-$$"
HOST_PORT="${SMOKE_HOST_PORT:-18080}"
BASE_URL="http://127.0.0.1:${HOST_PORT}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BOOK_JSON="${REPO_ROOT}/epub-generator/src/test/resources/generator/ancientgodlymonarch.json"
COVER_JPG="${REPO_ROOT}/epub-generator/src/test/resources/generator/ancientgodlymonarch.jpg"
EPUB_OUT="$(mktemp)"
REPORT_OUT="$(mktemp)"

if command -v podman >/dev/null 2>&1 && ! command -v docker >/dev/null 2>&1; then
  RUNTIME=(podman)
elif command -v docker >/dev/null 2>&1; then
  RUNTIME=(docker)
else
  echo "Neither docker nor podman found on PATH" >&2
  exit 1
fi

cleanup() {
  "${RUNTIME[@]}" rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
  rm -f "${EPUB_OUT}" "${REPORT_OUT}"
}
trap cleanup EXIT

echo "Starting ${IMAGE_TAG} as ${CONTAINER_NAME} on port ${HOST_PORT}"
"${RUNTIME[@]}" run -d --name "${CONTAINER_NAME}" -p "${HOST_PORT}:8080" "${IMAGE_TAG}" >/dev/null

echo "Waiting for service..."
ready=0
for _ in $(seq 1 60); do
  if curl -sf "${BASE_URL}/api/v1/epub/reports" >/dev/null; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "${ready}" != "1" ]]; then
  echo "Service did not become ready in time" >&2
  "${RUNTIME[@]}" logs "${CONTAINER_NAME}" >&2 || true
  exit 1
fi

BOOK_ID="$(python -c 'import json,sys; print(json.load(open(sys.argv[1]))["id"])' "${BOOK_JSON}")"

HTTP_CODE="$(curl -sS -o "${EPUB_OUT}" -w "%{http_code}" \
  -F "book=<${BOOK_JSON};type=application/json" \
  -F "cover=@${COVER_JPG};type=image/jpeg" \
  "${BASE_URL}/api/v1/epub")"

if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "POST /api/v1/epub failed with HTTP ${HTTP_CODE}" >&2
  cat "${EPUB_OUT}" >&2 || true
  exit 1
fi

python -c 'import sys; data=open(sys.argv[1],"rb").read(); sys.exit(0 if data[:2]==b"PK" and len(data)>100 else 1)' "${EPUB_OUT}"

HTTP_CODE="$(curl -sS -o "${REPORT_OUT}" -w "%{http_code}" \
  "${BASE_URL}/api/v1/epub/reports?bookId=${BOOK_ID}")"
if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "GET report by bookId failed with HTTP ${HTTP_CODE}" >&2
  cat "${REPORT_OUT}" >&2 || true
  exit 1
fi

python -c 'import json,sys; report=json.load(open(sys.argv[1])); assert report.get("bookId")==sys.argv[2], report; print("report ok", report.get("bookId"))' \
  "${REPORT_OUT}" "${BOOK_ID}"

HTTP_CODE="$(curl -sS -o "${REPORT_OUT}" -w "%{http_code}" "${BASE_URL}/api/v1/epub/reports")"
if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "GET all reports failed with HTTP ${HTTP_CODE}" >&2
  exit 1
fi

python -c 'import json,sys; data=json.load(open(sys.argv[1])); assert isinstance(data, list), data; print("all reports", len(data))' \
  "${REPORT_OUT}"

echo "Smoke test passed for ${IMAGE_TAG}"
