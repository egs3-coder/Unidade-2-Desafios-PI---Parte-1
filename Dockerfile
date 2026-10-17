# syntax=docker/dockerfile:1
#
# AgendaSaúde — imagem em dois estágios (multi-stage build).
#   Estágio 1 "build": JDK + Maven compilam e rodam os TESTES UNITÁRIOS. Teste vermelho = build falha = sem imagem.
#   Estágio 2 "runtime": só a JRE e o .jar. Nada de Maven, código-fonte ou JDK em produção.
#
#   docker build -t agendasaude .
#   docker build --build-arg GIT_SHA=$(git rev-parse HEAD) -t agendasaude .

# ---------- Estágio 1: compila + testa ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primeiro só o pom.xml: as dependências viram uma camada própria e ficam em cache
# enquanto o pom não mudar. Mudou só código? O build pula este passo.
COPY pom.xml .
RUN mvn -B -q -ntp dependency:go-offline

COPY src ./src
# "package" roda os testes unitários (surefire). Os *IT precisam de Docker e rodam na esteira.
RUN mvn -B -q -ntp package

# ---------- Estágio 2: só o necessário para rodar ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Nunca rodar como root dentro do container.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/agendasaude.jar app.jar
USER app

# Versão gravada na imagem pela esteira (SHA do commit); o smoke test compara com /api/versao.
ARG GIT_SHA=local
ENV APP_VERSION=${GIT_SHA} \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# wget já vem no Alpine (busybox); curl não. PORT é definido pelo Render; localmente vale 8080.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"status":"UP"' || exit 1

# exec: o Java vira o PID 1 e recebe o SIGTERM do "docker stop" (desligamento gracioso).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
