#!/bin/sh
set -euo pipefail

configure_tomcat() {
  local base_dir="$1"
  local http_port="$2"
  local shutdown_port="$3"

  local server_xml="${base_dir}/conf/server.xml"
  if [ -f "${server_xml}" ]; then
    sed -i "0,/Server port=\"[0-9]\+\"/s//Server port=\"${shutdown_port}\"/" "${server_xml}"
    sed -i "0,/<Connector port=\"[0-9]\+\" protocol=\"HTTP\/1.1\"/s//<Connector port=\"${http_port}\" protocol=\"HTTP\/1.1\"/" "${server_xml}"
  fi
}

API_HTTP_PORT=${API_HTTP_PORT:-8080}
API_SHUTDOWN_PORT=${API_SHUTDOWN_PORT:-8005}
ADMIN_HTTP_PORT=${ADMIN_HTTP_PORT:-8081}
ADMIN_SHUTDOWN_PORT=${ADMIN_SHUTDOWN_PORT:-8006}
WEB_STATIC_HTTP_PORT=${WEB_STATIC_HTTP_PORT:-8083}
WEB_STATIC_SHUTDOWN_PORT=${WEB_STATIC_SHUTDOWN_PORT:-8007}

if [ "${SERVICE:-api}" = "suite" ]; then
  export CATALINA_API_HOME=/opt/tomcat-api
  export CATALINA_ADMIN_HOME=/opt/tomcat-admin
  export CATALINA_STATIC_HOME=/opt/tomcat-static

  configure_tomcat "${CATALINA_API_HOME}" "${API_HTTP_PORT}" "${API_SHUTDOWN_PORT}"
  configure_tomcat "${CATALINA_ADMIN_HOME}" "${ADMIN_HTTP_PORT}" "${ADMIN_SHUTDOWN_PORT}"
  configure_tomcat "${CATALINA_STATIC_HOME}" "${WEB_STATIC_HTTP_PORT}" "${WEB_STATIC_SHUTDOWN_PORT}"

  mkdir -p /var/log/app-suite

  (
    export CATALINA_HOME=${CATALINA_API_HOME}
    export CATALINA_BASE=${CATALINA_API_HOME}
    cd ${CATALINA_API_HOME}
    nohup ./bin/catalina.sh run >> /var/log/app-suite/api.log 2>&1 &
  )

  (
    export CATALINA_HOME=${CATALINA_ADMIN_HOME}
    export CATALINA_BASE=${CATALINA_ADMIN_HOME}
    cd ${CATALINA_ADMIN_HOME}
    nohup ./bin/catalina.sh run >> /var/log/app-suite/admin.log 2>&1 &
  )

  (
    export CATALINA_HOME=${CATALINA_STATIC_HOME}
    export CATALINA_BASE=${CATALINA_STATIC_HOME}
    cd ${CATALINA_STATIC_HOME}
    nohup ./bin/catalina.sh run >> /var/log/app-suite/web-static.log 2>&1 &
  )

  [ -f /app-batch.jar ] && nohup java -jar /app-batch.jar >> /var/log/app-suite/batch.log 2>&1 &
  [ -f /app-worker.jar ] && nohup java -jar /app-worker.jar >> /var/log/app-suite/worker.log 2>&1 &

  while true; do
    LOG_FILES=""
    for f in /var/log/app-suite/*.log; do
      if [ -f "$f" ]; then
        LOG_FILES="$LOG_FILES $f"
      fi
    done

    if [ -n "$LOG_FILES" ]; then
      tail -f $LOG_FILES
      break
    fi

    sleep 2
  done
else
  if [ "${RUNTIME_KIND:-java}" = "tomcat" ]; then
    export CATALINA_BASE=${CATALINA_HOME}
    configure_tomcat "${CATALINA_BASE}" "${API_HTTP_PORT}" "${API_SHUTDOWN_PORT}"
    exec ${CATALINA_HOME}/bin/catalina.sh run
  else
    exec java -jar /app/app.jar
  fi
fi

