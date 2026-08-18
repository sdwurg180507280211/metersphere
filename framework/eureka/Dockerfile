FROM registry.cn-qingdao.aliyuncs.com/metersphere/alpine-openjdk17-jre

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG MS_VERSION=dev
ARG DEPENDENCY=target/dependency

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app


ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_MAIN_CLASS=io.metersphere.eureka.EurekaApplication
ENV MS_VERSION=${MS_VERSION}
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -Djasypt.encryptor.password=abcdMS123%"
# 配置密码加密（Jasypt）主密码已内置在 JAVA_OPTIONS：-Djasypt.encryptor.password=abcdMS123%
# ⚠️ 主密码在镜像内（docker history 可见明文）；若运行时覆盖 JAVA_OPTIONS 必须保留该参数
# 替代注入方式：环境变量 JASYPT_ENCRYPTOR_PASSWORD（优先级低于 -D，设置后以 -D 为准）


CMD ["/deployments/run-java.sh"]
