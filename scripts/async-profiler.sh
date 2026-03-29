#!/usr/bin/env bash
set -euo pipefail

# --- Configuration (override via env vars) ---
PROFILE_EVENT="${PROFILE_EVENT:-cpu}"       # cpu, alloc, wall
WRK_THREADS="${WRK_THREADS:-4}"
WRK_CONNECTIONS="${WRK_CONNECTIONS:-400}"
WRK_DURATION="${WRK_DURATION:-30s}"
PORT="${SERVER_PORT:-4221}"
BASE_URL="http://localhost:${PORT}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_DIR="${PROJECT_DIR}/build/profile-results"

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
if ! command -v asprof &>/dev/null; then
    fail "async-profiler is not installed. Install with: brew install async-profiler"
    exit 1
fi

if ! command -v wrk &>/dev/null; then
    fail "wrk is not installed. Install with: brew install wrk"
    exit 1
fi

case "${PROFILE_EVENT}" in
    cpu|alloc|wall) ;;
    *)
        fail "Invalid PROFILE_EVENT: ${PROFILE_EVENT}. Must be one of: cpu, alloc, wall"
        exit 1
        ;;
esac

# --- Build ---
info "Building server..."
cd "${PROJECT_DIR}"
./gradlew installDist --quiet 2>&1
ok "Build complete."

# --- Prepare results dir ---
mkdir -p "${RESULTS_DIR}"

# --- Start server with profiling-friendly JVM flags ---
info "Starting server on port ${PORT} (with profiling JVM flags)..."
export JAVA_OPTS="-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints"
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

# --- Build asprof event args ---
ASPROF_ARGS=(-e "${PROFILE_EVENT}")
if [[ "${PROFILE_EVENT}" == "wall" ]]; then
    ASPROF_ARGS=(--wall 1ms)
fi

OUTPUT_FILE="${RESULTS_DIR}/${PROFILE_EVENT}-flamegraph.html"

# --- Profile ---
echo ""
echo -e "${BOLD}============================================${NC}"
echo -e "${BOLD}  Profiling: ${PROFILE_EVENT}${NC}"
echo -e "${BOLD}  Load: wrk -t${WRK_THREADS} -c${WRK_CONNECTIONS} -d${WRK_DURATION}${NC}"
echo -e "${BOLD}============================================${NC}"

info "Starting profiler (event=${PROFILE_EVENT})..."
asprof start "${ASPROF_ARGS[@]}" "${SERVER_PID}"
ok "Profiler attached."

# --- Run load ---
info "Running wrk load on /echo/hello ..."
wrk -t"${WRK_THREADS}" -c"${WRK_CONNECTIONS}" -d"${WRK_DURATION}" \
    --latency \
    "${BASE_URL}/echo/hello"

# --- Stop profiler and dump flame graph ---
info "Stopping profiler and generating flame graph..."
asprof stop -f "${OUTPUT_FILE}" "${SERVER_PID}"
ok "Flame graph saved to: ${OUTPUT_FILE}"

echo ""
echo -e "${BOLD}============================================${NC}"
echo -e "${GREEN}${BOLD}  Profiling complete.${NC}"
echo -e "${BOLD}  Output: ${OUTPUT_FILE}${NC}"
echo -e "${BOLD}============================================${NC}"
echo ""
echo -e "  Open with: ${CYAN}open ${OUTPUT_FILE}${NC}"
