# =============================================================================
# Stage 1 — build: Node.js + Maven
# =============================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY package.json package-lock.json ./
RUN npm ci

COPY src/ src/
COPY tailwind.config.js postcss.config.js ./

RUN mkdir -p target/classes/static/css && npm run build:postcss
RUN ./mvnw package -DskipTests -B -Pprecompile-jte

# =============================================================================
# Stage 2 — runtime: JRE slim
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S sigaubs && adduser -S -G sigaubs sigaubs

WORKDIR /app

COPY --from=builder /app/target/SCCUBS-0.0.1-SNAPSHOT.jar app.jar
RUN chown sigaubs:sigaubs app.jar

USER sigaubs

# ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
