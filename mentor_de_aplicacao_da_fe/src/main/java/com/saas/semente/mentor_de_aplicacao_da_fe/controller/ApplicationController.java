package com.saas.semente.mentor_de_aplicacao_da_fe.controller;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.FaithApplication;
import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import com.saas.semente.mentor_de_aplicacao_da_fe.service.FaithApplicationService;
import com.saas.semente.mentor_de_aplicacao_da_fe.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional; // <-- IMPORT ADICIONADO
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ApplicationController {

    // Apenas injeção de Services
    private final FaithApplicationService applicationService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    public ApplicationController(FaithApplicationService applicationService, UserService userService) {
        this.applicationService = applicationService;
        this.userService = userService;
    }

    /**
     * Rota principal: / (Diário de Aplicações)
     */
    @GetMapping("/")
    @Transactional(readOnly = true) // <-- ANOTAÇÃO ADICIONADA AQUI
    public String index(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            return "redirect:/lading-page-saas";
        }

        User user = userService.getAuthenticatedUser(); // Delega a obtenção do User

        // Delega a busca de dados ao Service
        List<FaithApplication> applications = applicationService.getAllApplicationsByUserId(user.getId());
        model.addAttribute("applications", applications);

        return "index";
    }

    /**
     * Rota POST: /generate (Foco: Flow Control e Feedback)
     */
    @PostMapping("/generate")
    public String generateMentorship(@RequestParam("userChallenge") String userChallenge, RedirectAttributes ra) {
        if (userChallenge.isBlank()) {
            ra.addFlashAttribute("errorMessage", "O Desafio do usuário não pode ser vazio.");
            return "redirect:/";
        }

        User user = userService.getAuthenticatedUser();

        // 1. LÓGICA DE CONTROLE DE LIMITE (Pre-check e reset) - Delega ao Service
        userService.checkAndResetMonthlyPromptCount(user);

        int currentCount = user.getMonthlyPromptCount(); // Pega a contagem atualizada (após potencial reset)
        int limit = user.getSubscriptionPlan().getMonthlyLimit();

        // 2. Checagem final de limite (lógica de apresentação/feedback)
        if (limit != Integer.MAX_VALUE && currentCount >= limit) {
            ra.addFlashAttribute("errorMessage",
                    "Você atingiu o limite de " + limit + " mentorias neste mês (" + user.getSubscriptionPlan().name() + "). Considere fazer upgrade para o Plano Jardineiro (Ilimitado)!");
            return "redirect:/";
        }

        try {
            // 3. Executa a lógica de geração, incrementando o contador internamente no Service
            applicationService.generateAndSaveMentorship(userChallenge, user);

            // Pega o novo contador (já incrementado pelo Service) para o feedback
            int newCount = user.getMonthlyPromptCount();

            ra.addFlashAttribute("successMessage",
                    "Mentoria gerada e registrada com sucesso! Você usou " + newCount + " de " + (limit == Integer.MAX_VALUE ? "∞" : limit) + " prompts neste mês."); // Ajuste para exibir ∞ se for ilimitado
        } catch (Exception e) {
            // Mensagem de erro apenas no console, para não expor a API
            logger.error("Erro ao chamar a API do Gemini ou processar JSON: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Erro ao gerar a mentoria. Verifique sua chave de API ou a conexão.");
        }

        return "redirect:/";
    }

    /**
     * Rota: /dashboard (Foco: Mapeamento de Modelo)
     */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        User user = userService.getAuthenticatedUser();

        // 1. Obtém todos os registros do usuário
        List<FaithApplication> allRecords = applicationService.getAllApplicationsByUserId(user.getId());

        // 2. Delega o cálculo e agrupamento de dados para o Service
        Map<String, Object> dashboardData = applicationService.getDashboardData(allRecords);

        // 3. Adiciona os atributos ao modelo
        model.addAllAttributes(dashboardData);

        return "dasboard"; // Note: o nome do template é "dasboard.html", não "dashboard.html"
    }

    /**
     * Rota: /all-records
     */
    @GetMapping("/all-records")
    @Transactional(readOnly = true)
    public String allRecords(Model model) {
        User user = userService.getAuthenticatedUser();

        List<FaithApplication> applications = applicationService.getAllApplicationsByUserId(user.getId());
        model.addAttribute("applications", applications);
        return "all-records";
    }

    /**
     * Rota AJAX: /record/{id} (Foco: Resposta HTTP)
     */
    @GetMapping("/record/{id}")
    @ResponseBody
    @Transactional(readOnly = true) // <-- ANOTAÇÃO TAMBÉM NECESSÁRIA AQUI para ler LOB via AJAX
    public ResponseEntity<FaithApplication> getRecordById(@PathVariable("id") Long id) {
        User user = userService.getAuthenticatedUser();

        // Delega a busca e o filtro de segurança ao Service
        Optional<FaithApplication> app = applicationService.getApplicationByIdAndUserId(id, user.getId());

        return app.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rota de EXCLUSÃO: /delete/{id} (Foco: Confirmação e Redirect)
     * Não precisa de @Transactional aqui, pois o delete já é transacional por padrão via JpaRepository
     */
    @RequestMapping(value = "/delete/{id}", method = { RequestMethod.DELETE, RequestMethod.POST })
    public String deleteRecord(@PathVariable("id") Long id, RedirectAttributes ra) {
        User user = userService.getAuthenticatedUser();

        // Delega a lógica de exclusão e verificação de propriedade ao Service
        boolean deleted = applicationService.deleteApplication(id, user.getId());

        if (deleted) {
            ra.addFlashAttribute("successMessage", "Registro excluído com sucesso!");
        } else {
            ra.addFlashAttribute("errorMessage", "Erro: Registro não encontrado ou você não tem permissão para excluí-lo.");
        }

        // Redireciona para /all-records, pois é onde a exclusão acontece (melhor UX)
        return "redirect:/all-records";
    }
}