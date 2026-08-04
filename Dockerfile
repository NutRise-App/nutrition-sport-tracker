# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Dependency katmanını source değişikliklerinden ayırarak cache kullan.
COPY pom.xml .

RUN mvn -B -ntp dependency:go-offline

# Production image için yalnızca main source gerekli.
COPY src/main ./src/main

# Testler yerelde ve CI'da mvn verify ile çalıştırılıyor.
# Docker image build aşamasında tekrar çalıştırılmıyor.
RUN mvn -B -ntp -DskipTests clean package


FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Uygulamayı root olmayan sabit UID/GID ile çalıştır.
RUN groupadd --system --gid 10001 nutrise \
    && useradd \
        --system \
        --uid 10001 \
        --gid nutrise \
        --home-dir /app \
        --shell /usr/sbin/nologin \
        nutrise

COPY --from=build \
    --chown=10001:10001 \
    /workspace/target/*.jar \
    /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
