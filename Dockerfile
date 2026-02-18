# =============================================================================
# Stage 1 — build: Node.js + Maven
# =============================================================================
FROM eclipse-temurin:25-jdk-jammy AS builder

# Instalar Node.js 20.x
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar Maven wrapper e POM (camada cacheável)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Baixar dependências Maven (cache de camada)
RUN ./mvnw dependency:go-offline -B

# Instalar dependências npm
COPY package.json package-lock.json ./
RUN npm ci

# Copiar código-fonte e configs
COPY src/ src/
COPY tailwind.config.js postcss.config.js ./

# 1) Gerar CSS em target/classes/ (PostCSS escreve direto lá)
RUN mkdir -p target/classes/static/css && npm run build:postcss

# 2) Empacotar JAR SEM clean (preserva o CSS gerado no passo anterior)
RUN ./mvnw package -DskipTests -B

# =============================================================================
# Stage 2 — runtime: JRE slim
# =============================================================================
FROM eclipse-temurin:25-jre-jammy AS runtime

RUN groupadd --system sigaubs && useradd --system --gid sigaubs sigaubs

WORKDIR /app

COPY --from=builder /app/target/SCCUBS-0.0.1-SNAPSHOT-dev.jar app.jar
RUN chown sigaubs:sigaubs app.jar

USER sigaubs

ENV SPRING_PROFILES_ACTIVE=prd
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
