#!/usr/bin/env bash
set -euo pipefail

# --- Configuration (override via env vars) ---
THREADS="${WRK_THREADS:-4}"
CONNECTIONS="${WRK_CONNECTIONS:-400}"
DURATION="${WRK_DURATION:-30s}"
PORT="${SERVER_PORT:-4221}"
BASE_URL="http://localhost:${PORT}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; }

SERVER_PID=""

cleanup() {
    if [[ -n "${SERVER_PID}" ]]; then
        info "Stopping server (pid ${SERVER_PID})..."
        kill "${SERVER_PID}" 2>/dev/null || true
        wait "${SERVER_PID}" 2>/dev/null || true
        ok "Server stopped."
    fi
}
trap cleanup EXIT

# --- Pre-flight ---
if ! command -v wrk &>/dev/null; then
    fail "wrk is not installed. Install with: brew install wrk"
    exit 1
fi

# --- Build ---
info "Building server..."
cd "${PROJECT_DIR}"
./gradlew installDist --quiet 2>&1
ok "Build complete."

# --- Start server ---
info "Starting server on port ${PORT}..."
"${PROJECT_DIR}/app/build/install/app/bin/app" &
SERVER_PID=$!

# --- Wait for readiness ---
info "Waiting for server to be ready..."
RETRIES=30
for i in $(seq 1 ${RETRIES}); do
    if curl -sf "${BASE_URL}/" >/dev/null 2>&1; then
        ok "Server is ready (attempt ${i}/${RETRIES})."
        break
    fi
    if [[ ${i} -eq ${RETRIES} ]]; then
        fail "Server did not become ready after ${RETRIES} attempts."
        exit 1
    fi
    sleep 0.5
done

# --- Run load tests ---
echo ""
echo -e "${BOLD}============================================${NC}"
echo -e "${BOLD}  Load Test: wrk -t${THREADS} -c${CONNECTIONS} -d${DURATION}${NC}"
echo -e "${BOLD}============================================${NC}"

run_bench() {
    local label="$1"
    local path="$2"
    echo ""
    echo -e "${BOLD}--- ${label}: ${BASE_URL}${path} ---${NC}"
    wrk -t"${THREADS}" -c"${CONNECTIONS}" -d"${DURATION}" \
        --latency \
        "${BASE_URL}${path}"
}

run_bench "GET /"              "/"
run_bench "GET /echo/hello"    "/echo/hello"
run_bench "GET /user-agent"    "/user-agent"

echo ""
echo -e "${BOLD}============================================${NC}"
echo -e "${GREEN}${BOLD}  Load test complete.${NC}"
echo -e "${BOLD}============================================${NC}"
