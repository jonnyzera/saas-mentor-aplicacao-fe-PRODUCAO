package com.saas.semente.mentor_de_aplicacao_da_fe.controller;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.saas.semente.mentor_de_aplicacao_da_fe.model.SubscriptionPlan; 
import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import com.saas.semente.mentor_de_aplicacao_da_fe.service.EmailService;
import com.saas.semente.mentor_de_aplicacao_da_fe.service.MercadoPagoService;
import com.saas.semente.mentor_de_aplicacao_da_fe.service.SubscriptionService; 
import com.saas.semente.mentor_de_aplicacao_da_fe.service.UserService; 

import jakarta.servlet.http.HttpSession;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;

import java.util.Optional; 
import java.util.regex.Pattern;

@Controller
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private static final String REDIRECT_MY_PLAN = "redirect:/my-plan";
    private static final String ATTRIBUTE_ERROR_MESSAGE = "errorMessage";

    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    
    private static final String[] ALLOWED_DOMAINS = {
        "@gmail.com", "@googlemail.com", "@outlook.com", "@hotmail.com", 
        "@yahoo.com", "@yahoo.com.br", "@ymail.com", "@icloud.com", 
        "@bol.com.br", "@uol.com.br"
    };

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final MercadoPagoService mercadoPagoService;
    
    public PaymentController(UserService userService, SubscriptionService subscriptionService, EmailService emailService, MercadoPagoService mercadoPagoService){
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.mercadoPagoService = mercadoPagoService;
    }

    @GetMapping("/my-plan")
    public String myPlan(Model model) {
        User user = userService.getAuthenticatedUser(); 

        if (!user.isEnabled()) {
            return "redirect:/checkout/processing";
        }
        
        model.addAttribute("subscriptionPlans", SubscriptionPlan.values());

        int currentCount = user.getMonthlyPromptCount();
        int limit = user.getSubscriptionPlan().getMonthlyLimit();
        String planName = user.getSubscriptionPlan().name();
        int usagePercentage = subscriptionService.calculateUsagePercentage(user);

        model.addAttribute("planName", planName);
        model.addAttribute("currentCount", currentCount);
        model.addAttribute("limit", limit);
        model.addAttribute("usagePercentage", usagePercentage);

        return "meuplano";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam("plan") String planName, Model model) {
        model.addAttribute("planName", planName.toUpperCase());
        
        if (!model.containsAttribute("name")) {
            model.addAttribute("name", "");
        }
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        if (!model.containsAttribute("identificationNumber")) {
            model.addAttribute("identificationNumber", "");
        }
        
        return "checkout"; 
    }

    @PostMapping("/checkout/process")
    public String processCheckout(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword, 
            @RequestParam("identificationNumber") String identificationNumber,
            @RequestParam("planName") String planName, 
            Model model,
            RedirectAttributes ra,
            HttpSession session) { 
        
        session.setAttribute("checkoutData_name", name);
        session.setAttribute("checkoutData_email", email);
        session.setAttribute("checkoutData_cpf", identificationNumber);
        session.setAttribute("checkoutData_planName", planName);

        String cpfLimpo = identificationNumber.replaceAll("[^0-9]", "");

        Runnable preencherModeloComErro = () -> {
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("identificationNumber", identificationNumber); 
            model.addAttribute("planName", planName);
        };

        // ... (Validações 1 a 5) ...
        // 1. Validação de Nome Completo
        if (!name.trim().contains(" ")) {
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "Por favor, insira seu **nome completo** (nome e sobrenome) para cadastro.");
            preencherModeloComErro.run();
            return "checkout"; 
        }

        // 2. Validação de CPF
        if (cpfLimpo.length() != 11) { 
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "O CPF inserido é inválido. Por favor, verifique.");
            preencherModeloComErro.run();
            return "checkout"; 
        }

        // 3. Validação de Senha Forte
        if (!pattern.matcher(password).matches()) {
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "A senha é fraca. Ela deve conter no mínimo 8 caracteres, incluindo pelo menos **1 letra maiúscula, 1 minúscula e 1 número**.");
            preencherModeloComErro.run();
            return "checkout"; 
        }

        // 4. Validação de Confirmação de Senha
        if (!password.equals(confirmPassword)) {
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "As senhas não coincidem. Por favor, digite novamente.");
            preencherModeloComErro.run();
            return "checkout"; 
        }

        // 5. Validação de Domínio de Email
        boolean domainIsValid = false;
        for (String domain : ALLOWED_DOMAINS) {
            if (email.toLowerCase().endsWith(domain)) {
                domainIsValid = true;
                break;
            }
        }
        if (!domainIsValid) {
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "O domínio do e-mail inserido não é válido ou suportado...");
            preencherModeloComErro.run();
            return "checkout"; 
        }


        // 6. LÓGICA DE VERIFICAÇÃO (MODIFICADA)
        Optional<User> existingUserOpt = userService.findByEmail(email);
        User userToProcess; 

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            
            if (existingUser.isEnabled()) {
                model.addAttribute("accountExistsError", "O e-mail **" + email + "** já está registrado e ativo. Por favor, <a th:href='@{/login}' href='/login' class='alert-link'>faça login</a> para gerenciar seu plano.");
                preencherModeloComErro.run();
                return "checkout"; 
            } else {
                logger.info("Reutilizando registro inativo para o email: {}", email);
                userToProcess = subscriptionService.updateInactiveUser(existingUser, name, password, cpfLimpo, planName);
            }
            
        } else {
            logger.info("Criando novo registro inativo (pré-registro) para o email: {}", email);
            userToProcess = subscriptionService.preRegisterUser(name, email, password, cpfLimpo, planName);
        }

        // --- FLUXO DE SUCESSO ---
        try {
            Preference preference = mercadoPagoService.createPreference(
                planName, 
                userToProcess.getEmail(), 
                userToProcess.getName(), 
                userToProcess.getIdentificationNumber(),
                "REG" // Indica que é um novo REGISTRO
            );
            
            return "redirect:" + preference.getInitPoint(); 
            
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, e.getMessage());
            preencherModeloComErro.run();
            return "checkout"; 
        } 
          catch (MPApiException e) { 
            logger.error("Erro na API do Mercado Pago (MPApiException - Status {}): {}", e.getStatusCode(), e.getMessage(), e);
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "Erro de comunicação com o Mercado Pago. Verifique suas credenciais.");
            preencherModeloComErro.run();
            return "checkout"; 
        } catch (MPException e) {
            logger.error("Erro no SDK do Mercado Pago (MPException): {}", e.getMessage(), e);
            model.addAttribute(ATTRIBUTE_ERROR_MESSAGE, "Erro interno do serviço de pagamento. Tente novamente.");
            preencherModeloComErro.run();
            return "checkout"; 
        }
    }
    
    @GetMapping("/checkout/processing")
    public String processingPayment(Model model) {
        model.addAttribute("title", "Pagamento em Processamento");
        model.addAttribute("message", "Aguardando a confirmação final do pagamento pelo Mercado Pago. Você receberá um e-mail de boas-vindas assim que sua conta for ativada (cerca de 1 minuto). Você pode fechar esta página.");
        return "processing"; 
    }


    /**
     * NOVO MÉTODO: Rota GET para onde o Mercado Pago redireciona
     * em caso de pagamento FALHO ou CANCELADO (clicar em "Voltar").
     * Este método restaura os dados do formulário da sessão.
     */
    @GetMapping("/checkout/cancel")
    public String checkoutCancel(HttpSession session, RedirectAttributes ra) {
        
        String name = (String) session.getAttribute("checkoutData_name");
        String email = (String) session.getAttribute("checkoutData_email");
        String cpf = (String) session.getAttribute("checkoutData_cpf");
        String planName = (String) session.getAttribute("checkoutData_planName");

        session.removeAttribute("checkoutData_name");
        session.removeAttribute("checkoutData_email");
        session.removeAttribute("checkoutData_cpf");
        session.removeAttribute("checkoutData_planName");

        ra.addFlashAttribute("name", name);
        ra.addFlashAttribute("email", email);
        ra.addFlashAttribute("identificationNumber", cpf);
        ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "O pagamento foi cancelado. Seus dados foram restaurados, por favor, tente novamente.");

        if (planName == null) {
            return "redirect:/lading-page-saas";
        }
        
        return "redirect:/checkout?plan=" + planName; 
    }

    // ****** INÍCIO DA CORREÇÃO (NOVO MÉTODO PARA CANCELAMENTO DE UPGRADE) ******
    /**
     * NOVO MÉTODO: Rota GET para onde o Mercado Pago redireciona
     * em caso de falha ou cancelamento de um UPGRADE (usuário já logado).
     */
    @GetMapping("/upgrade/cancel")
    public String upgradeCancel(RedirectAttributes ra) {
        
        // 1. Adiciona a mensagem de erro/aviso
        ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "O pagamento do upgrade foi cancelado. Seu plano atual foi mantido.");

        // 2. Redireciona de volta para a página de gerenciamento de plano
        return REDIRECT_MY_PLAN; // "redirect:/my-plan"
    }
    // ****** FIM DA CORREÇÃO ******


    /**
     * Rota POST para processar um UPGRADE de plano.
     * Este método agora envia o usuário para o pagamento, em vez de
     * fazer o upgrade diretamente.
     */
    @PostMapping("/upgrade-plan")
    public String upgradePlan(
            @RequestParam("planName") String planName,
            RedirectAttributes ra) {
        
        User user = userService.getAuthenticatedUser(); 
        
        try {
            // 1. Validação (Lógica movida do SubscriptionService para cá)
            SubscriptionPlan newPlan = SubscriptionPlan.getByName(planName);
            if (user.getSubscriptionPlan().getMonthlyLimit() >= newPlan.getMonthlyLimit()) {
                ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "Não é possível fazer downgrade ou selecionar o mesmo plano (" + planName + ").");
                return REDIRECT_MY_PLAN;
            }

            // 2. Criar a Preferência de Pagamento para o UPGRADE
            Preference preference = mercadoPagoService.createPreference(
                planName,                 // O *novo* plano
                user.getEmail(),          // Email do usuário logado
                user.getName(),           // Nome do usuário logado
                user.getIdentificationNumber(), // CPF do usuário logado
                "UPG"                     // Indica que é um UPGRADE
            );
            
            // 3. Redireciona para o Mercado Pago
            return "redirect:" + preference.getInitPoint(); 
            
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "Plano de assinatura inválido.");
        } 
        catch (MPApiException e) { 
            logger.error("Erro na API do Mercado Pago (Upgrade MPApiException - Status {}): {}", e.getStatusCode(), e.getMessage(), e);
            ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "Erro de comunicação com o Mercado Pago. Tente novamente.");
        } catch (MPException e) {
            logger.error("Erro no SDK do Mercado Pago (Upgrade MPException): {}", e.getMessage(), e);
            ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "Erro interno do serviço de pagamento. Tente novamente.");
        } catch (Exception e) {
            logger.error("Erro ao processar o upgrade de plano: {}", e.getMessage(), e);
            ra.addFlashAttribute(ATTRIBUTE_ERROR_MESSAGE, "Ocorreu um erro ao processar o upgrade. Tente novamente.");
        }
        
        return REDIRECT_MY_PLAN; 
    }
}