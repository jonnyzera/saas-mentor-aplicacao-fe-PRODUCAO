# 1. Fase de Build (Compilação do JAR)
# Use uma imagem Maven com Java 17
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Define o diretório de trabalho raiz do repositório
WORKDIR /app

# COPIA TUDO do diretório raiz para o WORKDIR /app
COPY . .

# CRÍTICO: Mudar o WORKDIR para o subdiretório onde está o pom.xml
WORKDIR /app/mentor_de_aplicacao_da_fe

# Executa o build do Maven. Assume o 'app.jar' se o pom.xml foi ajustado.
RUN mvn clean package -DskipTests

# 2. Fase de Execução (Runtime)
# Imagem base leve com apenas o JRE para execução
FROM eclipse-temurin:17-jre-alpine

# Expõe a porta que o Spring Boot usará.
EXPOSE 8080

# Copia o JAR do diretório 'target' da fase de build, renomeando para 'app.jar'
# O caminho está correto para buscar no subdiretório do build.
COPY --from=build /app/mentor_de_aplicacao_da_fe/target/app.jar app.jar

# Comando de execução: Inicia a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]