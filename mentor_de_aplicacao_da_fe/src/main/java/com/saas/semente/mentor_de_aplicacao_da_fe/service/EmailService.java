package com.saas.semente.mentor_de_aplicacao_da_fe.service;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia um e-mail de boas-vindas com o link de confirmação (simulado).
     * @param user O novo usuário.
     */
    public void sendWelcomeAndConfirmationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Bem-vindo ao Semente! Confirme seu E-mail.");
            
            // Simulação de um link de confirmação. Na prática, um token seria gerado.
            String confirmationLink = "http://localhost:8080/confirm?token=SIMULATED_TOKEN";

            String text = String.format(
                "Olá %s,\n\n" +
                "Sua assinatura do plano %s foi ativada com sucesso! Bem-vindo(a) à jornada de crescimento.\n\n" +
                "Para garantir a segurança de sua conta e validar seu acesso, por favor, clique no link abaixo para confirmar seu e-mail:\n" +
                "%s\n\n" +
                "Seu login é seu e-mail: %s\n\n" +
                "Comece sua aplicação da fé agora mesmo.\n\n" +
                "Atenciosamente,\n" +
                "Time Semente - Mentor de Aplicação da Fé",
                user.getName(), user.getSubscriptionPlan().name(), confirmationLink, user.getEmail()
            );

            message.setText(text);
            mailSender.send(message);

            logger.info("E-mail de boas-vindas enviado para: {}", user.getEmail());

        } catch (Exception e) {
            // O processo de pagamento NÃO deve falhar em função de um erro de e-mail.
            logger.error("Falha ao enviar e-mail de boas-vindas para {}: {}", user.getEmail(), e.getMessage());
        }
    }
}