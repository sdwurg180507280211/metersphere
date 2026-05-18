FROM registry.cn-qingdao.aliyuncs.com/metersphere/fabric8-java-alpine-openjdk17-jre

LABEL maintainer="support@fit2cloud.com"

ARG MS_VERSION=dev
ARG DEPENDENCY=backend/target/dependency

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_MAIN_CLASS=io.metersphere.analyticsstat.AnalyticsStatApplication
ENV AB_OFF=true
ENV MS_VERSION=${MS_VERSION}
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED"

CMD ["/deployments/run-java.sh"]
