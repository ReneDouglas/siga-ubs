# syntax=docker/dockerfile:1.7
FROM node:20.20.2-bookworm-slim AS assets
WORKDIR /assets
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY tailwind.config.js postcss.config.js ./
COPY src/main/jte/ src/main/jte/
COPY src/main/resources/static/ src/main/resources/static/
RUN mkdir -p target/classes/static/css && npm run build:postcss

FROM eclipse-temurin:21.0.8_9-jdk-jammy
RUN groupadd --gid 10001 sigaubs \
    && useradd --uid 10001 --gid sigaubs --create-home --shell /usr/sbin/nologin sigaubs
WORKDIR /app
COPY --chown=sigaubs:sigaubs .mvn/ .mvn/
COPY --chown=sigaubs:sigaubs mvnw pom.xml ./
RUN chmod +x mvnw && mkdir -p jte-classes target \
    && chown -R sigaubs:sigaubs /app
USER 10001:10001
RUN ./mvnw dependency:go-offline -B
COPY --chown=sigaubs:sigaubs src/ src/
COPY --chown=sigaubs:sigaubs --from=assets /assets/target/classes/static/css/ target/classes/static/css/
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
EXPOSE 8080 9090
ENTRYPOINT ["sh", "-c", "./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=\"$JAVA_OPTS\""]
