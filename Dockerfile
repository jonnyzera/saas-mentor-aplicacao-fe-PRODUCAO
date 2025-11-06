# 1. Fase de Build (Compilação do JAR)
# Use a imagem oficial do Maven/Java 17 para compilar
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o arquivo de configuração do projeto (pom.xml)
# O pom.xml está no diretório atual, pois este Dockerfile está em 'mentor_de_aplicacao_da_fe/'
COPY pom.xml .

# Copia o código fonte
COPY src ./src

# Executa o build do Maven. O '-DskipTests' é opcional para builds mais rápidos.
RUN mvn package -DskipTests

# 2. Fase de Execução (Runtime)
# Use uma imagem mais leve (apenas Java Runtime Environment) para a execução final
FROM eclipse-temurin:17-jre-alpine

# Define o nome do arquivo JAR gerado
ARG JAR_FILE=target/mentor_de_aplicacao_da_fe-0.0.1-SNAPSHOT.jar

# Copia o JAR da fase de build para a fase de execução
COPY --from=build /app/${JAR_FILE} app.jar

# Define o ponto de entrada e o comando de execução
# O Spring Boot automaticamente lerá a variável de ambiente PORT que o Railway define.
ENTRYPOINT ["java", "-jar", "app.jar"]