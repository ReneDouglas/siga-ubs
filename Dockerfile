# =============================================================================
# Runtime de desenvolvimento — Node.js + Maven + JDK
# =============================================================================
FROM eclipse-temurin:21-jdk-jammy

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
RUN mkdir -p jte-classes

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=\"$JAVA_OPTS\""]
