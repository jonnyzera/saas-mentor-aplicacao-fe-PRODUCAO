package com.saas.semente.mentor_de_aplicacao_da_fe.service;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.FaithApplication;
import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import com.saas.semente.mentor_de_aplicacao_da_fe.repository.FaithApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient; 

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FaithApplicationService {

    // BASE URL ajustada para o WebClient usar na construção
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final FaithApplicationRepository repository;
    private final UserService userService;
    private final WebClient webClient; // NOVO: Injeção do WebClient

    public FaithApplicationService(FaithApplicationRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
        // Configura o WebClient (Singleton)
        this.webClient = WebClient.builder() 
                               .baseUrl(GEMINI_API_BASE_URL)
                               .build();
    }
    
    /**
     * Gera a mentoria chamando a API, incrementa o contador e salva a aplicação.
     */
    @Transactional
    public FaithApplication generateAndSaveMentorship(String userChallenge, User user) throws Exception {
        
        FaithApplication newApp = callGeminiApi(userChallenge);
        
        // Associa o usuário e incrementa o contador (lógica de negócio combinada)
        newApp.setUser(user);
        userService.incrementPromptCount(user);
        
        return repository.save(newApp);
    }
    
    /**
     * Lógica de Chamada da API Gemini (usando WebClient).
     */
    @SuppressWarnings("unchecked")// Adicionado para suprimir os avisos de cast no corpo da respota da API
    private FaithApplication callGeminiApi(String userChallenge) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 1. Definição do System Instruction (Persona e Regras)
        // ****** INÍCIO DA CORREÇÃO (Prompt Atualizado) ******
        String systemPrompt = "Você é o 'Mentor de Aplicação da Fé', um conselheiro cristão sábio e amigável. Seu objetivo é pegar o 'Desafio do Usuário' e, com base em princípios bíblicos, gerar uma mentoria estruturada em 5 partes (Versículo, Reflexão, Conselhos Práticos, Referências e Oração). Sua 'Reflexão Aplicada' deve ter um tom pessoal e caloroso. Os 'Conselhos Práticos' devem ser 3 ações concretas e diretas que o usuário pode tomar HOJE, baseadas na reflexão. Retorne a resposta APENAS como um objeto JSON que segue o schema fornecido.";
        // ****** FIM DA CORREÇÃO ******

        // 2. Definição do Schema JSON para a resposta
        // ****** INÍCIO DA CORREÇÃO (Schema Atualizado) ******
        String jsonSchema = """
                {
                  "type": "OBJECT",
                  "properties": {
                    "identifiedTheme": { "type": "STRING", "description": "Tema(s) central(is) identificado(s) no desafio do usuário. Ex: Ansiedade, Perdão, Direção." },
                    "versiculoBussola": { "type": "STRING", "description": "O versículo bíblico central para o desafio. Ex: Filipenses 4:6-7" },
                    "reflexaoAplicada": { "type": "STRING", "description": "A reflexão e conselho com tom de amigo. Use quebras de linha (\\n) para formatar parágrafos." },
                    "conselhosPraticos": { "type": "STRING", "description": "Três conselhos práticos e acionáveis (numerados ou com marcadores) baseados no versículo e reflexão. Use quebras de linha (\\n) para formatar." },
                    "referenciasCruzadas": { "type": "STRING", "description": "Outras referências bíblicas contextuais para estudo. Ex: Mateus 6:34, 1 Pedro 5:7, Salmos 46:1" },
                    "oracaoSemente": { "type": "STRING", "description": "Uma oração curta e poderosa baseada no desafio e no versículo. Use quebras de linha (\\n) para formatar." }
                  }
                }
                """;
        // ****** FIM DA CORREÇÃO ******

        // 3. Montagem do corpo da requisição
        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> userPart = Map.of("text", userChallenge);
        Map<String, Object> contents = Map.of("role", "user", "parts", List.of(userPart));
        payload.put("contents", List.of(contents));

        Map<String, Object> systemInstruction = Map.of("parts", List.of(Map.of("text", systemPrompt)));
        payload.put("systemInstruction", systemInstruction);

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", objectMapper.readValue(jsonSchema, Map.class));
        payload.put("generationConfig", generationConfig);

        // --- 4. EXECUÇÃO DA CHAMADA COM WEBCLIENT (Substituição do RestTemplate) ---
        Map<String, Object> responseBody = webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/models/gemini-2.5-flash-preview-05-20:generateContent")
                .queryParam("key", geminiApiKey)
                .build())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            // Trata status de erro HTTP (ex: 400 ou 500)
            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                // Lança exceção customizada com base no status de erro
                return clientResponse.bodyToMono(String.class)
                    .map(errorBody -> new Exception("Erro na API Gemini (" + clientResponse.statusCode() + "): " + errorBody));
            })
            // Extrai o corpo da resposta como um Map<String, Object>
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block(); // Bloqueia a execução (mantendo o método síncrono)

        // 6. Processamento da Resposta (extração do JSON)
        if (responseBody == null || !responseBody.containsKey("candidates")) {
            throw new Exception("Resposta da API Gemini inválida ou vazia.");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");

        if (candidates.isEmpty()) {
            throw new Exception("Nenhum candidato de resposta da API Gemini.");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");

        if (parts.isEmpty() || !parts.get(0).containsKey("text")) {
            throw new Exception("Conteúdo da resposta Gemini está vazio ou malformado.");
        }

        String jsonText = parts.get(0).get("text");

        Map<String, String> result = objectMapper.readValue(jsonText, Map.class);

        // 7. Mapeamento para o Objeto FaithApplication
        FaithApplication app = new FaithApplication();
        app.setUserChallenge(userChallenge);
        app.setIdentifiedTheme(result.get("identifiedTheme"));
        app.setVersiculoBussola(result.get("versiculoBussola"));
        app.setReflexaoAplicada(result.get("reflexaoAplicada"));
        // ****** INÍCIO DA CORREÇÃO (Mapeamento do Novo Campo) ******
        app.setConselhosPraticos(result.get("conselhosPraticos")); 
        // ****** FIM DA CORREÇÃO ******
        app.setReferenciasCruzadas(result.get("referenciasCruzadas"));
        app.setOracaoSemente(result.get("oracaoSemente"));

        return app;
    }
    
    // --- Lógica de Consulta e Dashboard ---

    public List<FaithApplication> getAllApplicationsByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<FaithApplication> getApplicationByIdAndUserId(Long id, Long userId) {
        return repository.findById(id)
                // Garante que a aplicação pertence ao usuário logado (Camada de Segurança)
                .filter(app -> app.getUser().getId().equals(userId));
    }
    
    @Transactional
    public boolean deleteApplication(Long id, Long userId) {
        Optional<FaithApplication> appToDelete = repository.findById(id);
        
        if (appToDelete.isPresent() && appToDelete.get().getUser().getId().equals(userId)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Gera todos os dados necessários para a tela do dashboard.
     */
    public Map<String, Object> getDashboardData(List<FaithApplication> allRecords) {
        
        List<Integer> registrationDates = allRecords.stream()
                .map(app -> app.getCreatedAt().getDayOfMonth())
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, List<FaithApplication>> recordsByDay = allRecords.stream()
                .collect(Collectors.groupingBy(app -> app.getCreatedAt().getDayOfMonth()));

        Map<String, Long> themeFrequency = allRecords.stream()
                .flatMap(app -> {
                    if (app.getIdentifiedTheme() != null) {
                        return List.of(app.getIdentifiedTheme().split(",")).stream()
                                .map(String::trim);
                    }
                    return null;
                })
                .filter(theme -> theme != null && !theme.isBlank())
                .collect(Collectors.groupingBy(theme -> theme, Collectors.counting()));

        String latestInsight = generateLatestInsight(themeFrequency);
        
        List<String> lastThreeUniqueThemes = allRecords.stream()
            .map(FaithApplication::getIdentifiedTheme)
            .filter(theme -> theme != null && !theme.isBlank())
            .flatMap(theme -> List.of(theme.split(",")).stream().map(String::trim))
            .distinct()
            .limit(3)
            .collect(Collectors.toList());


        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("registrationDates", registrationDates);
        dashboardData.put("themeFrequency", themeFrequency);
        dashboardData.put("recordsByDay", recordsByDay);
        dashboardData.put("latestInsight", latestInsight);
        dashboardData.put("lastThreeUniqueThemes", lastThreeUniqueThemes);

        return dashboardData;
    }


    private String generateLatestInsight(Map<String, Long> themeFrequency) {
        if (themeFrequency.isEmpty()) {
            return null;
        }

        String mainTheme = themeFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Reflexão");

        long totalRecords = themeFrequency.values().stream().mapToLong(Long::longValue).sum();

        return String.format(
                "Você tem %d registros de aplicações neste período, e seu foco principal tem sido em <strong>%s</strong>. O Mentor sugere que você releia o Salmo 23 para encontrar descanso e direção. Continue a jornada!",
                totalRecords, mainTheme);
    }
}