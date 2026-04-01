#!/usr/bin/env bash
set -euo pipefail

# --- Configuration (override via env vars) ---
REQUESTS="${HEY_REQUESTS:-100000}"
CONCURRENCY_LEVELS="${HEY_CONCURRENCY:-10 100 1000}"
PORT="${SERVER_PORT:-4221}"
GC_COLLECTOR="${GC_COLLECTOR:-}"  # e.g., "-XX:+UseZGC" or "-XX:+UseShenandoahGC"
BASE_URL="http://localhost:${PORT}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_DIR="${PROJECT_DIR}/build/gc-analysis-results"

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
        SERVER_PID=""
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

# --- Prepare results dir ---
mkdir -p "${RESULTS_DIR}"

# --- Summary collection ---
SUMMARY_FILE="${RESULTS_DIR}/summary.txt"
: > "${SUMMARY_FILE}"

GC_LABEL="default (G1GC)"
if [[ -n "${GC_COLLECTOR}" ]]; then
    GC_LABEL="${GC_COLLECTOR}"
fi

echo ""
echo -e "${BOLD}====================================================${NC}"
echo -e "${BOLD}  GC Log Analysis: ${REQUESTS} requests per run${NC}"
echo -e "${BOLD}  Collector: ${GC_LABEL}${NC}"
echo -e "${BOLD}  Concurrency sweep: ${CONCURRENCY_LEVELS}${NC}"
echo -e "${BOLD}====================================================${NC}"

for c in ${CONCURRENCY_LEVELS}; do
    echo ""
    echo -e "${BOLD}--- Concurrency: ${c} ---${NC}"

    GC_LOG="${RESULTS_DIR}/gc_c${c}.log"
    HEY_OUTPUT="${RESULTS_DIR}/hey_c${c}.txt"

    # Build JAVA_OPTS
    JVM_GC_FLAGS="-Xlog:gc*:file=${GC_LOG}:time,level,tags"
    if [[ -n "${GC_COLLECTOR}" ]]; then
        JVM_GC_FLAGS="${GC_COLLECTOR} ${JVM_GC_FLAGS}"
    fi

    # --- Start server with GC logging ---
    info "Starting server with GC logging (c=${c})..."
    export JAVA_OPTS="${JVM_GC_FLAGS}"
    "${PROJECT_DIR}/app/build/install/app/bin/app" &
    SERVER_PID=$!

    # --- Wait for readiness ---
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

    # --- Run load test ---
    info "Running hey: ${REQUESTS} requests, concurrency ${c}..."
    hey -n "${REQUESTS}" -c "${c}" "${BASE_URL}/echo/hello" > "${HEY_OUTPUT}" 2>&1
    ok "Load test complete."

    # Extract req/s from hey output
    RPS=$(grep 'Requests/sec' "${HEY_OUTPUT}" | awk '{print $2}')

    # --- Stop server ---
    info "Stopping server..."
    kill "${SERVER_PID}" 2>/dev/null || true
    wait "${SERVER_PID}" 2>/dev/null || true
    ok "Server stopped."
    SERVER_PID=""

    # --- Parse GC log ---
    info "Parsing GC log..."

    # Extract pause durations in milliseconds from GC log.
    # Java unified logging format: lines containing "pause" with timing like "1.234ms"
    # Covers both "Pause Young" and "Pause Full" entries.
    PAUSE_MS_FILE="${RESULTS_DIR}/pauses_c${c}.txt"
    grep -iE 'gc\([0-9]+\).*pause.*(Young|Full|Remark|Cleanup)' "${GC_LOG}" \
        | grep -oE '[0-9]+\.[0-9]+ms' \
        | sed 's/ms$//' \
        | sort -n > "${PAUSE_MS_FILE}" || true

    PAUSE_COUNT=$(wc -l < "${PAUSE_MS_FILE}" | tr -d ' ')

    if [[ "${PAUSE_COUNT}" -eq 0 ]]; then
        warn "No GC pauses detected at c=${c} (may indicate ZGC/Shenandoah with no STW pauses)."
        echo "${c}|0|0|0|0|0|${RPS}" >> "${SUMMARY_FILE}"
        sleep 1
        continue
    fi

    # Compute stats using awk
    read -r MIN_P AVG_P MAX_P P99_P TOTAL_STW <<< "$(awk '
    BEGIN { min = 999999; max = 0; sum = 0; n = 0 }
    {
        n++
        val = $1 + 0
        sum += val
        if (val < min) min = val
        if (val > max) max = val
        values[n] = val
    }
    END {
        if (n == 0) { print "0 0 0 0 0"; exit }
        avg = sum / n
        p99_idx = int(n * 0.99)
        if (p99_idx < 1) p99_idx = 1
        printf "%.3f %.3f %.3f %.3f %.3f\n", min, avg, max, values[p99_idx], sum
    }
    ' "${PAUSE_MS_FILE}")"

    ok "Pauses: ${PAUSE_COUNT}  avg=${AVG_P}ms  max=${MAX_P}ms  p99=${P99_P}ms  total_stw=${TOTAL_STW}ms"

    echo "${c}|${PAUSE_COUNT}|${AVG_P}|${MAX_P}|${P99_P}|${TOTAL_STW}|${RPS}" >> "${SUMMARY_FILE}"

    # Brief cooldown between runs
    sleep 2
done

# --- Print summary table ---
echo ""
echo -e "${BOLD}====================================================${NC}"
echo -e "${BOLD}  GC Analysis Summary — ${GC_LABEL}${NC}"
echo -e "${BOLD}====================================================${NC}"
printf "${BOLD}%-8s %8s %10s %10s %10s %12s %12s${NC}\n" \
    "Conc." "Pauses" "Avg(ms)" "Max(ms)" "P99(ms)" "STW(ms)" "Req/s"
echo "------------------------------------------------------------------------"

while IFS='|' read -r conc pauses avg_p max_p p99_p total_stw rps; do
    printf "%-8s %8s %10s %10s %10s %12s %12s\n" \
        "${conc}" "${pauses}" "${avg_p}" "${max_p}" "${p99_p}" "${total_stw}" "${rps}"
done < "${SUMMARY_FILE}"

echo ""
echo -e "${GREEN}${BOLD}  GC analysis complete. Results in: ${RESULTS_DIR}${NC}"
echo ""
echo -e "  Raw GC logs:   ${CYAN}${RESULTS_DIR}/gc_c*.log${NC}"
echo -e "  Pause data:    ${CYAN}${RESULTS_DIR}/pauses_c*.txt${NC}"
echo -e "  hey output:    ${CYAN}${RESULTS_DIR}/hey_c*.txt${NC}"
echo ""
echo -e "  ${BOLD}Compare collectors:${NC}"
echo -e "    ${CYAN}GC_COLLECTOR=\"-XX:+UseZGC\" ./scripts/gc-analysis.sh${NC}"
echo -e "    ${CYAN}GC_COLLECTOR=\"-XX:+UseShenandoahGC\" ./scripts/gc-analysis.sh${NC}"
