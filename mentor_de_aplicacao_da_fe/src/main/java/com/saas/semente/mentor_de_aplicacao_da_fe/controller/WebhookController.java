package com.saas.semente.mentor_de_aplicacao_da_fe.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;

import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.saas.semente.mentor_de_aplicacao_da_fe.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Deve ser um RestController para lidar com JSON/POST
@RequestMapping("/mercadopago")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final SubscriptionService subscriptionService;

    @Value("${mercadopago.access.token}")
    private String mpAccessToken;

    public WebhookController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
       
    }

    /**
     * Recebe notificações (Webhooks) do Mercado Pago.
     * Esta rota deve ser pública e configurada no painel do MP.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleMercadoPagoWebhook(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "topic", required = false) String topic) {

        // O Mercado Pago envia o ID do recurso e o tópico.
        // Se o tópico não for 'payment', ignora (ex: merchant_order, chargebacks)
        if (id == null || !"payment".equals(topic)) {
            logger.info("Webhook ignorado. Tópico: {} | ID: {}", topic, id);
            return ResponseEntity.ok().build(); 
        }

        try {
            // 1. Inicializa o SDK e busca a transação real no MP (Busca defensiva)
            MercadoPagoConfig.setAccessToken(mpAccessToken);
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.valueOf(id));
            
            // 2. CRÍTICO: Verifica se o status é 'approved'
            if ("approved".equals(payment.getStatus())) {
                String externalReference = payment.getExternalReference();
                
                // 3. Ativa o usuário usando o External Reference
                // ****** INÍCIO DA CORREÇÃO (MUDANÇA DE NOME DO MÉTODO) ******
                subscriptionService.processPaymentConfirmation(externalReference); 
                // ****** FIM DA CORREÇÃO ******

                logger.info("Pagamento APROVADO e Processado. ID MP: {} | External Reference: {}", id, externalReference);
                
                // O Webhook não deve fazer login ou redirecionar, apenas processar.
                // O email de boas-vindas deve ser enviado dentro do processPaymentConfirmation
                
            } else if ("rejected".equals(payment.getStatus())) {
                // Opcional: Aqui você pode notificar o usuário da rejeição ou manter o usuário inativo.
                logger.warn("Pagamento REJEITADO. ID MP: {} | External Reference: {}", id, payment.getExternalReference());
            }

        } catch (MPException e) {
            logger.error("Erro na API do Mercado Pago ao buscar o pagamento {}: {}", id, e.getMessage());
            // Retornar 500 para que o MP tente reenviar
            return ResponseEntity.internalServerError().build(); 
        } catch (Exception e) {
            logger.error("Erro ao processar a ativação/upgrade do usuário (Webhook): {}", e.getMessage(), e);
             // Retornar 500 para que o MP tente reenviar
            return ResponseEntity.internalServerError().build();
        }

        // 4. Retorna 200 OK para o Mercado Pago, confirmando que a notificação foi recebida.
        return ResponseEntity.ok().build();
    }
}