# 🌿 Semente: Mentor de Aplicação da Fé (SaaS)

## 🌟 Descrição do Projeto

O projeto **Semente** é uma aplicação web SaaS (Software as a Service) completa, construída com Spring Boot, Thymeleaf e Spring Security. Ela funciona como um "Mentor Premium" para a aplicação da fé, permitindo que usuários cadastrados transformem desafios pessoais em crescimento espiritual.

O usuário, após escolher um plano e realizar o pagamento, compartilha um desafio (emoção, problema, conflito). O sistema, utilizando uma integração com a API Generativa do Google (Gemini), gera uma mentoria completa e acionável. A aplicação gerencia diferentes níveis de acesso baseados em planos de assinatura (Semente, Colheita, Jardineiro) e processa pagamentos de forma segura via Mercado Pago.

## ✨ Funcionalidades Principais

* **Autenticação e Segurança:** Sistema de login e registro completo usando **Spring Security**. Os usuários têm acesso protegido às suas páginas pessoais.
* **Fluxo de Pagamento (SaaS):**
    * **Checkout:** Página de checkout onde o usuário escolhe um plano, insere dados (Nome, CPF, Email, Senha) e é redirecionado para o **Mercado Pago**.
    * **Planos de Assinatura:** Três níveis de planos: `SEMENTE` (10 mentorias/mês), `COLHEITA` (30 mentorias/mês) e `JARDINEIRO` (ilimitado).
    * **Webhooks:** Um endpoint (`/mercadopago/webhook`) recebe notificações do Mercado Pago para ativar a conta do usuário (ou processar upgrades) assim que o pagamento é aprovado.
* **Geração de Mentoria (IA):**
    * O `FaithApplicationService` chama a API do Google Gemini.
    * A IA recebe o desafio do usuário e um *prompt* de sistema que a instrui a agir como um "Mentor de Aplicação da Fé".
    * A resposta é estruturada em JSON contendo: `identifiedTheme`, `versiculoBussola`, `reflexaoAplicada`, `conselhosPraticos`, `referenciasCruzadas` e `oracaoSemente`.
* **Diário de Aplicações (`/`):** Página principal (`index.html`) onde o usuário autenticado insere seu desafio. O sistema verifica seu limite de prompts mensal antes de gerar a mentoria.
* **Gerenciamento de Plano (`/my-plan`):** Página (`meuplano.html`) onde o usuário pode ver seu plano atual, verificar o uso de mentorias e realizar **upgrade** de plano.
* **Mapa de Crescimento (`/dashboard`):** Painel visual (`dasboard.html`) que mostra:
    * Um gráfico de composição de temas (`themeFrequency`) analisados pela IA.
    * Um calendário de hábito (`registrationDates`) destacando os dias com registros.
    * Um *Insight Reflexivo* sobre o progresso.
* **Arquivo de Registros (`/all-records`):** Exibe a lista completa (`all-records.html`) de todas as mentorias salvas, permitindo visualização detalhada e exclusão.

## 🛠️ Stack Tecnológico

| Componente | Detalhe | Arquivos de Referência |
| :--- | :--- | :--- |
| **Linguagem** | Java 17 | `pom.xml` |
| **Framework** | Spring Boot 3.5.7 (Web, Data JPA, Security) | `pom.xml` |
| **Banco de Dados** | PostgreSQL | `application.properties` |
| **Interface** | Thymeleaf, Bootstrap 5.3.3, ApexCharts, Font Awesome | `pom.xml`, `index.html`, `dasboard.html` |
| **Pagamentos** | SDK do Mercado Pago (sdk-java) | `pom.xml`, `MercadoPagoService.java` |
| **IA Generativa**| Google Gemini (via WebClient) | `FaithApplicationService.java`, `application.properties` |
| **Segurança** | Spring Security (com BCrypt) | `SecurityConfig.java`, `CustomUserDetailsService.java` |
| **Comunicação** | Spring Boot Mail (para e-mails de boas-vindas) | `EmailService.java`, `application.properties` |
| **Ferramenta de Build** | Apache Maven (com Maven Wrapper) | `pom.xml`, `mvnw` |

## 🚀 Configuração e Execução

### Pré-requisitos

1.  Java Development Kit (JDK) **17** ou superior.
2.  Um servidor de banco de dados **PostgreSQL** ativo.
3.  Uma conta no **Mercado Pago** (para obter o Access Token).
4.  Uma chave de API do **Google Gemini**.
5.  (Recomendado) **Ngrok** ou similar para testar os Webhooks do Mercado Pago localmente.

### 1. Configuração do Banco de Dados

Crie um banco de dados no PostgreSQL (ex: `mentor_db`) e um usuário (ex: `mentor_user`) com uma senha. Atualize o arquivo `src/main/resources/application.properties`:

```properties
# Configuração do PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/mentor_db
spring.datasource.username=mentor_user
spring.datasource.password=123456
spring.jpa.hibernate.ddl-auto=update
