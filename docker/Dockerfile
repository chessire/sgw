# syntax=docker/dockerfile:1.4

ARG SERVICE=api
ARG OUTPUT_TYPE=war
ARG RUNTIME_KIND=tomcat
ARG MAVEN_IMAGE=maven:3.9.7-eclipse-temurin-17
ARG ROCKY_IMAGE=rockylinux:9-minimal
ARG TOMCAT_VERSION=10.1.28
ARG TOMCAT_IMAGE=tomcat:${TOMCAT_VERSION}-jdk17-temurin

FROM ${MAVEN_IMAGE} AS builder

WORKDIR /workspace

COPY . .

ARG SERVICE
ARG OUTPUT_TYPE
RUN if [ "${SERVICE}" = "suite" ]; then \
      mvn -pl app/api,app/admin,app/batch,app/worker,app/web-static -am -DskipTests clean package; \
      find app/api/target -maxdepth 1 -type f -name "*.war" -exec cp {} /workspace/api.war \; ; \
      find app/admin/target -maxdepth 1 -type f -name "*.war" -exec cp {} /workspace/admin.war \; ; \
      find app/web-static/target -maxdepth 1 -type f -name "*.war" -exec cp {} /workspace/web-static.war \; ; \
      find app/batch/target -maxdepth 1 -type f -name "*.jar" ! -name "*sources.jar" ! -name "*javadoc.jar" -exec cp {} /workspace/batch.jar \; ; \
      find app/worker/target -maxdepth 1 -type f -name "*.jar" ! -name "*sources.jar" ! -name "*javadoc.jar" -exec cp {} /workspace/worker.jar \; ; \
    else \
      mvn -pl app/${SERVICE} -am -DskipTests clean package; \
      if [ "${OUTPUT_TYPE}" = "war" ]; then \
         ARTIFACT_PATH=$(find app/${SERVICE}/target -maxdepth 1 -type f -name "*.war"); \
       else \
         ARTIFACT_PATH=$(find app/${SERVICE}/target -maxdepth 1 -type f -name "*.jar" ! -name "*sources.jar" ! -name "*javadoc.jar"); \
       fi; \
      cp "${ARTIFACT_PATH}" /workspace/application.artifact; \
    fi

FROM ${TOMCAT_IMAGE} AS tomcat

FROM ${ROCKY_IMAGE} AS runner

ARG SERVICE
ARG RUNTIME_KIND
ARG TOMCAT_VERSION

ENV TZ=Asia/Seoul \
    RUNTIME_KIND=${RUNTIME_KIND} \
    CATALINA_HOME=/usr/local/tomcat \
    JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:$PATH

RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo ${TZ} > /etc/timezone

COPY --from=builder /workspace /workspace
COPY --from=tomcat /opt/java/openjdk /opt/java/openjdk
COPY --from=tomcat /usr/local/tomcat /opt/tomcat-dist

RUN if [ "${SERVICE}" = "suite" ]; then \
      mkdir -p /opt/tomcat-api /opt/tomcat-admin /opt/tomcat-static && \
      cp -r /opt/tomcat-dist/* /opt/tomcat-api/ && \
      cp -r /opt/tomcat-dist/* /opt/tomcat-admin/ && \
      cp -r /opt/tomcat-dist/* /opt/tomcat-static/ && \
      rm -rf /opt/tomcat-api/webapps/* /opt/tomcat-admin/webapps/* /opt/tomcat-static/webapps/* && \
      mv /workspace/api.war /opt/tomcat-api/webapps/ROOT.war && \
      mv /workspace/admin.war /opt/tomcat-admin/webapps/ROOT.war && \
      mv /workspace/web-static.war /opt/tomcat-static/webapps/ROOT.war && \
      mv /workspace/batch.jar /app-batch.jar && \
      mv /workspace/worker.jar /app-worker.jar; \
    else \
      if [ "${RUNTIME_KIND}" = "tomcat" ]; then \
        mkdir -p ${CATALINA_HOME} && \
        cp -r /opt/tomcat-dist/* ${CATALINA_HOME}/ && \
        rm -rf ${CATALINA_HOME}/webapps/* && \
        mv /workspace/application.artifact ${CATALINA_HOME}/webapps/ROOT.war; \
      else \
        mv /workspace/application.artifact /app/app.jar; \
      fi; \
    fi

RUN rm -rf /opt/tomcat-dist

COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080 8081 8082 8083 8084

ENTRYPOINT ["/entrypoint.sh"]

