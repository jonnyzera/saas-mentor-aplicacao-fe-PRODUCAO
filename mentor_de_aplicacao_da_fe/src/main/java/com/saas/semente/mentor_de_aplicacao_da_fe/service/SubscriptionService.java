package com.saas.semente.mentor_de_aplicacao_da_fe.service;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.SubscriptionPlan;
import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import com.saas.semente.mentor_de_aplicacao_da_fe.repository.UserRepository;
// ****** INÍCIO DA CORREÇÃO (NOVOS IMPORTS) ******
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ****** FIM DA CORREÇÃO ******
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    // ****** INÍCIO DA CORREÇÃO (LOGGER) ******
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);
    // ****** FIM DA CORREÇÃO ******
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Construtor corrigido: Injeta EmailService (necessário para o Webhook)
    public SubscriptionService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * NOVO MÉTODO: Pré-registra o usuário com status INATIVO (isEnabled=false).
     * Chamado pelo PaymentController antes de criar a preferência de pagamento.
     * @throws IllegalStateException Se o email já estiver registrado.
     * @return O User pré-registrado.
     */
    @Transactional
    public User preRegisterUser(String name, String email, String rawPassword, String cpf, String planName) { // 1. PARÂMETRO CPF ADICIONADO
        if (userRepository.existsByEmail(email)) {
            // Esta verificação agora é tratada principalmente no Controller,
            // mas é uma boa garantia de banco de dados.
            throw new IllegalStateException("O e-mail já está registrado.");
        }

        // Usa o método auxiliar getByName do Enum (implementado no passo anterior)
        SubscriptionPlan plan = SubscriptionPlan.getByName(planName);
        
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setIdentificationNumber(cpf); // 2. CPF É SALVO NO USUÁRIO
        newUser.setSubscriptionPlan(plan);
        newUser.setMonthlyPromptCount(0);
        newUser.setEnabled(false); // CRÍTICO: O usuário está inativo
        
        return userRepository.save(newUser);
    }
    
    /**
     * NOVO MÉTODO: Atualiza um usuário INATIVO se o cliente tentar
     * se registrar novamente (ex: abandonou o pagamento anterior).
     */
    @Transactional
    public User updateInactiveUser(User existingUser, String name, String rawPassword, String cpf, String planName) {
        SubscriptionPlan plan = SubscriptionPlan.getByName(planName);
        
        existingUser.setName(name);
        existingUser.setPassword(passwordEncoder.encode(rawPassword));
        existingUser.setIdentificationNumber(cpf);
        existingUser.setSubscriptionPlan(plan);
        // isEnabled continua false até o webhook confirmar o pagamento
        
        return userRepository.save(existingUser);
    }

    
    // ****** INÍCIO DA CORREÇÃO (MÉTODO RENOMEADO E COM LÓGICA DE UPGRADE) ******
    /**
     * Processa a confirmação de pagamento (via Webhook) para REGISTRO ou UPGRADE.
     * @param externalReference O ID no formato "TIPO-PLANO-EMAIL-UUID"
     * @throws IllegalStateException se o usuário ou o formato da referência forem inválidos.
     */
    @Transactional
    public void processPaymentConfirmation(String externalReference) {
        // Formato esperado: "TIPO-PLANO-EMAIL-UUID"
        String[] parts = externalReference.split("-");
        
        if (parts.length < 4) { // Deve ter pelo menos 4 partes (ex: UPG-PLANO-EMAIL-UUID)
             logger.error("External Reference malformatada recebida do Webhook: {}", externalReference);
            throw new IllegalStateException("External Reference malformatada: " + externalReference);
        }

        String type = parts[0];     // "REG" ou "UPG"
        String planName = parts[1]; // "SEMENTE", "COLHEITA", etc.
        String email = parts[2];    // "usuario@gmail.com"
        // O resto é UUID, que ignoramos aqui.

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado para ativação/upgrade via webhook. Email: " + email));

        SubscriptionPlan newPlan = SubscriptionPlan.getByName(planName);

        if ("REG".equals(type)) {
            // Lógica de ATIVAÇÃO (a que já existia)
            if (!user.isEnabled()) {
                user.setEnabled(true);
                user.setLastPromptMonthCheck(LocalDateTime.now()); // Ativa o ciclo mensal
                user.setSubscriptionPlan(newPlan); // Garante o plano correto
                userRepository.save(user);

                // Envia o E-mail de Boas-Vindas e Confirmação
                emailService.sendWelcomeAndConfirmationEmail(user);
                logger.info("Usuário ATIVADO (REG) para o plano {}: {}", planName, email);
            } else {
                logger.warn("Webhook de REGISTRO recebido para usuário já ativo: {}", email);
            }
            
        } else if ("UPG".equals(type)) {
            // Lógica de UPGRADE (a que faltava)
            logger.info("Iniciando UPGRADE para o plano {} para o usuário: {}", planName, email);
            user.setSubscriptionPlan(newPlan);
            user.setMonthlyPromptCount(0); // Reseta o contador no upgrade
            user.setLastPromptMonthCheck(LocalDateTime.now()); 
            userRepository.save(user);
            
            // (Opcional) Enviar um email de "Upgrade Confirmado"
            // emailService.sendUpgradeConfirmationEmail(user); 
            logger.info("Upgrade para {} concluído para o usuário: {}", planName, email);
            
        } else {
            logger.warn("Tipo de referência desconhecido no webhook: {} (Ref: {})", type, externalReference);
        }
    }
    // ****** FIM DA CORREÇÃO ******

    /**
     * Rota de Criação de Usuário Antiga (Marcada como obsoleta e substituída)
     */
    @Deprecated
    public User createAndActivateUserSubscription(String name, String email, String password, String planName) {
        throw new UnsupportedOperationException("Método obsoleto. Use preRegisterUser e processPaymentConfirmation.");
    }
    
    // ****** INÍCIO DA CORREÇÃO (MÉTODO REMOVIDO) ******
    /**
     * Lógica para fazer upgrade do plano de um usuário autenticado.
     * ESTE MÉTODO FOI REMOVIDO. A lógica agora é tratada pelo
     * PaymentController (que gera a cobrança) e pelo 
     * processPaymentConfirmation (que confirma o upgrade após o pagamento).
     */
    /*
    @Transactional
    public User upgradePlan(User user, String planName) {
        // ... LÓGICA ANTIGA REMOVIDA ...
    }
    */
    // ****** FIM DA CORREÇÃO ******
    
    /**
     * Calcula a porcentagem de uso mensal de prompts.
     */
    public int calculateUsagePercentage(User user) {
        int currentCount = user.getMonthlyPromptCount();
        int limit = user.getSubscriptionPlan().getMonthlyLimit();
        
        if (limit == Integer.MAX_VALUE) {
            return 0;
        } else if (limit > 0) {
            return (int) Math.min(100, (double) currentCount / limit * 100);
        }
        return 0;
    }
    
    /**
     * Verifica se o email já existe.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(String.valueOf(email));
    }
}