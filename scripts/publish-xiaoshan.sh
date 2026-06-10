#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_DIR="${PROJECT_ROOT}/server"

TARGET_HOST="${TARGET_HOST:-192.168.204.122}"
TARGET_USER="${TARGET_USER:-root}"
TARGET_DIR="${TARGET_DIR:-/data}"
REMOTE_JAR="${TARGET_DIR}/floating-ball-server.jar"
REMOTE_START_SCRIPT="${TARGET_DIR}/start-floating-ball-server.sh"
REMOTE_LOG_DIR="${TARGET_DIR}/floating-ball-server/logs"
RUN_TESTS="${RUN_TESTS:-0}"
START_AFTER_UPLOAD="${START_AFTER_UPLOAD:-0}"

if [[ "${RUN_TESTS}" == "1" ]]; then
  MAVEN_ARGS=(clean package)
else
  MAVEN_ARGS=(clean package -DskipTests)
fi

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

need_cmd mvn
need_cmd ssh
need_cmd scp

run_ssh() {
  local remote_cmd="$1"
  if [[ -n "${SSH_PASS:-}" ]]; then
    if command -v sshpass >/dev/null 2>&1; then
      SSHPASS="${SSH_PASS}" sshpass -e ssh -o StrictHostKeyChecking=accept-new "${TARGET_USER}@${TARGET_HOST}" "${remote_cmd}"
    elif command -v expect >/dev/null 2>&1; then
      TARGET="${TARGET_USER}@${TARGET_HOST}" REMOTE_CMD="${remote_cmd}" expect <<'EXPECT'
set timeout -1
spawn ssh -o StrictHostKeyChecking=accept-new $env(TARGET) $env(REMOTE_CMD)
expect {
  -re "(?i)password:" {
    send "$env(SSH_PASS)\r"
    exp_continue
  }
  eof
}
catch wait result
exit [lindex $result 3]
EXPECT
    else
      echo "SSH_PASS is set, but neither sshpass nor expect is available." >&2
      exit 1
    fi
  else
    ssh -o StrictHostKeyChecking=accept-new "${TARGET_USER}@${TARGET_HOST}" "${remote_cmd}"
  fi
}

run_scp() {
  local local_path="$1"
  local remote_path="$2"
  if [[ -n "${SSH_PASS:-}" ]]; then
    if command -v sshpass >/dev/null 2>&1; then
      SSHPASS="${SSH_PASS}" sshpass -e scp -o StrictHostKeyChecking=accept-new "${local_path}" "${TARGET_USER}@${TARGET_HOST}:${remote_path}"
    elif command -v expect >/dev/null 2>&1; then
      LOCAL_PATH="${local_path}" REMOTE_TARGET="${TARGET_USER}@${TARGET_HOST}:${remote_path}" expect <<'EXPECT'
set timeout -1
spawn scp -o StrictHostKeyChecking=accept-new $env(LOCAL_PATH) $env(REMOTE_TARGET)
expect {
  -re "(?i)password:" {
    send "$env(SSH_PASS)\r"
    exp_continue
  }
  eof
}
catch wait result
exit [lindex $result 3]
EXPECT
    else
      echo "SSH_PASS is set, but neither sshpass nor expect is available." >&2
      exit 1
    fi
  else
    scp -o StrictHostKeyChecking=accept-new "${local_path}" "${TARGET_USER}@${TARGET_HOST}:${remote_path}"
  fi
}

build_start_script() {
  local output="$1"
  cat > "${output}" <<'REMOTE_SCRIPT'
#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="floating-ball-server"
BASE_DIR="/data"
JAR_FILE="${BASE_DIR}/floating-ball-server.jar"
PID_FILE="${BASE_DIR}/${APP_NAME}.pid"
LOG_DIR="${BASE_DIR}/${APP_NAME}/logs"
CONSOLE_LOG="${LOG_DIR}/console.log"

JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-xiaoshan}"
EXTRA_ARGS="${EXTRA_ARGS:-}"

mkdir -p "${LOG_DIR}"

is_running() {
  [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" >/dev/null 2>&1
}

start() {
  if is_running; then
    echo "${APP_NAME} is already running, pid=$(cat "${PID_FILE}")"
    return 0
  fi
  if [[ ! -f "${JAR_FILE}" ]]; then
    echo "Jar file not found: ${JAR_FILE}" >&2
    exit 1
  fi

  nohup "${JAVA_BIN}" ${JAVA_OPTS} \
    -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
    -DLOG_PATH="${LOG_DIR}" \
    -jar "${JAR_FILE}" \
    --logging.file.path="${LOG_DIR}" \
    ${EXTRA_ARGS} >> "${CONSOLE_LOG}" 2>&1 &

  echo $! > "${PID_FILE}"
  echo "${APP_NAME} started, pid=$(cat "${PID_FILE}")"
  echo "Log file: ${LOG_DIR}/floating-ball-server.log"
  echo "Console log: ${CONSOLE_LOG}"
}

stop() {
  if ! is_running; then
    echo "${APP_NAME} is not running"
    rm -f "${PID_FILE}"
    return 0
  fi

  local pid
  pid="$(cat "${PID_FILE}")"
  kill "${pid}"
  for _ in $(seq 1 30); do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      rm -f "${PID_FILE}"
      echo "${APP_NAME} stopped"
      return 0
    fi
    sleep 1
  done

  echo "Force stopping ${APP_NAME}, pid=${pid}"
  kill -9 "${pid}" >/dev/null 2>&1 || true
  rm -f "${PID_FILE}"
}

status() {
  if is_running; then
    echo "${APP_NAME} is running, pid=$(cat "${PID_FILE}")"
  else
    echo "${APP_NAME} is not running"
  fi
}

logs() {
  local app_log="${LOG_DIR}/floating-ball-server.log"
  if [[ -f "${app_log}" ]]; then
    tail -n "${TAIL_LINES:-200}" -f "${app_log}"
  else
    tail -n "${TAIL_LINES:-200}" -f "${CONSOLE_LOG}"
  fi
}

case "${1:-start}" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    stop
    start
    ;;
  status)
    status
    ;;
  logs)
    logs
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status|logs}" >&2
    exit 1
    ;;
esac
REMOTE_SCRIPT
}

echo "Building server with Maven: mvn -f ${SERVER_DIR}/pom.xml ${MAVEN_ARGS[*]}"
mvn -f "${SERVER_DIR}/pom.xml" "${MAVEN_ARGS[@]}"

JAR_FILE="$(find "${SERVER_DIR}/target" -maxdepth 1 -type f -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | sort | tail -n 1)"
if [[ -z "${JAR_FILE}" || ! -f "${JAR_FILE}" ]]; then
  echo "Packaged jar not found under ${SERVER_DIR}/target" >&2
  exit 1
fi

TMP_START_SCRIPT="$(mktemp)"
trap 'rm -f "${TMP_START_SCRIPT}"' EXIT
build_start_script "${TMP_START_SCRIPT}"
chmod +x "${TMP_START_SCRIPT}"

echo "Preparing remote directory ${TARGET_USER}@${TARGET_HOST}:${TARGET_DIR}"
run_ssh "mkdir -p '${TARGET_DIR}' '${REMOTE_LOG_DIR}'"

echo "Uploading jar: ${JAR_FILE} -> ${TARGET_USER}@${TARGET_HOST}:${REMOTE_JAR}"
run_scp "${JAR_FILE}" "${REMOTE_JAR}"

echo "Uploading start script -> ${TARGET_USER}@${TARGET_HOST}:${REMOTE_START_SCRIPT}"
run_scp "${TMP_START_SCRIPT}" "${REMOTE_START_SCRIPT}"
run_ssh "chmod +x '${REMOTE_START_SCRIPT}' && ls -lh '${REMOTE_JAR}' '${REMOTE_START_SCRIPT}'"

if [[ "${START_AFTER_UPLOAD}" == "1" ]]; then
  echo "Restarting remote service"
  run_ssh "'${REMOTE_START_SCRIPT}' restart"
else
  echo "Upload complete. Start with: ssh ${TARGET_USER}@${TARGET_HOST} '${REMOTE_START_SCRIPT} start'"
fi
