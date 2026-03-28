#!/usr/bin/env bash
set -euo pipefail

# --- Configuration (override via env vars) ---
REQUESTS="${HEY_REQUESTS:-100000}"
CONCURRENCY_LEVELS="${HEY_CONCURRENCY:-10 100 1000}"
PORT="${SERVER_PORT:-4221}"
BASE_URL="http://localhost:${PORT}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_DIR="${PROJECT_DIR}/build/load-test-results"

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }

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
if ! command -v hey &>/dev/null; then
    fail "hey is not installed. Install with: brew install hey"
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

# --- Prepare results dir ---
mkdir -p "${RESULTS_DIR}"

# --- Endpoints to test ---
declare -a ENDPOINTS=("/" "/echo/hello" "/user-agent")
declare -a LABELS=("GET /" "GET /echo/hello" "GET /user-agent")

# --- Summary collection ---
# Format: label | concurrency | req/s | avg latency | p99 latency
SUMMARY_FILE="${RESULTS_DIR}/summary.txt"
: > "${SUMMARY_FILE}"

echo ""
echo -e "${BOLD}====================================================${NC}"
echo -e "${BOLD}  hey load test: ${REQUESTS} requests per run${NC}"
echo -e "${BOLD}  Concurrency sweep: ${CONCURRENCY_LEVELS}${NC}"
echo -e "${BOLD}====================================================${NC}"

for idx in "${!ENDPOINTS[@]}"; do
    endpoint="${ENDPOINTS[$idx]}"
    label="${LABELS[$idx]}"

    for c in ${CONCURRENCY_LEVELS}; do
        echo ""
        echo -e "${BOLD}--- ${label}  c=${c} ---${NC}"

        output_file="${RESULTS_DIR}/hey_$(echo "${endpoint}" | tr '/' '_')_c${c}.txt"

        hey -n "${REQUESTS}" -c "${c}" "${BASE_URL}${endpoint}" | tee "${output_file}"

        # Extract metrics from hey output
        rps=$(grep 'Requests/sec' "${output_file}" | awk '{print $2}')
        avg_latency=$(grep 'Average' "${output_file}" | head -1 | awk '{print $2}')
        p99_latency=$(grep '99%' "${output_file}" | head -1 | awk '{print $2}')

        echo "${label}|${c}|${rps}|${avg_latency}|${p99_latency}" >> "${SUMMARY_FILE}"

        # Brief cooldown between runs
        sleep 2
    done
done

# --- Print summary table ---
echo ""
echo -e "${BOLD}====================================================${NC}"
echo -e "${BOLD}  Summary${NC}"
echo -e "${BOLD}====================================================${NC}"
printf "${BOLD}%-20s %8s %12s %12s %12s${NC}\n" "Endpoint" "Conc." "Req/s" "Avg Lat." "P99 Lat."
echo "--------------------------------------------------------------------"

while IFS='|' read -r label conc rps avg p99; do
    printf "%-20s %8s %12s %12s %12s\n" "${label}" "${conc}" "${rps}" "${avg}" "${p99}"
done < "${SUMMARY_FILE}"

echo ""
echo -e "${GREEN}${BOLD}  Load test complete. Full results in: ${RESULTS_DIR}${NC}"
