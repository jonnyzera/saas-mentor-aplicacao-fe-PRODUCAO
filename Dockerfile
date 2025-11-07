# 1. Fase de Build (Compilação do JAR)
# Usa a imagem oficial do Maven com Java 17
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Define o diretório de trabalho
WORKDIR /app

# COPIAR TUDO: Copia todo o conteúdo do Root Directory (mentor_de_aplicacao_da_fe)
# para o diretório de trabalho do container. Isso resolve o erro de "src not found".
COPY . .

# Executa o build do Maven, pulando os testes para ser mais rápido.
RUN mvn package -DskipTests

# 2. Fase de Execução (Runtime)
# Usa uma imagem mais leve (apenas JRE 17) para execução
FROM eclipse-temurin:17-jre-alpine

# Define o nome do arquivo JAR gerado
# O nome do JAR é baseado no pom.xml
ARG JAR_FILE=target/mentor_de_aplicacao_da_fe-0.0.1-SNAPSHOT.jar

# Copia o JAR da fase de build para a fase de execução
COPY --from=build /app/${JAR_FILE} app.jar

# Define o ponto de entrada e o comando de execução
# O Spring Boot automaticamente lerá a variável de ambiente PORT que o Railway define.
ENTRYPOINT ["java", "-jar", "app.jar"]