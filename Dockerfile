# 1. Fase de Build (Compilação do JAR)
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Define o diretório de trabalho
WORKDIR /app

# COPIA TUDO: Isso resolve a falha de 'pom.xml not found'
COPY . .

# Executa o build do Maven, pulando os testes
RUN mvn package -DskipTests

# 2. Fase de Execução (Runtime)
FROM eclipse-temurin:17-jre-alpine

# Nome do JAR gerado pelo Maven
ARG JAR_FILE=target/mentor_de_aplicacao_da_fe-0.0.1-SNAPSHOT.jar

# Copia o JAR da fase de build
COPY --from=build /app/${JAR_FILE} app.jar

# Comando de execução
ENTRYPOINT ["java", "-jar", "app.jar"]