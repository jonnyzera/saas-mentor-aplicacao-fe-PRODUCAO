package com.saas.semente.mentor_de_aplicacao_da_fe.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
// Removidos os imports de PaymentMethods, pois não vamos mais filtrar
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.saas.semente.mentor_de_aplicacao_da_fe.model.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
// Removidos os imports de List e ArrayList

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access.token}")
    private String mpAccessToken;

    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;
    
    // Preço de R$ 1,00 para teste
    private static final BigDecimal SEMENTE_PRICE = new BigDecimal("1.00");
    private static final BigDecimal COLHEITA_PRICE = new BigDecimal("49.90");
    private static final BigDecimal JARDINEIRO_PRICE = new BigDecimal("99.90");

    /**
     * Cria a preferência de pagamento.
     * @param referenceType "REG" para novo registro, "UPG" para upgrade.
     */
    public Preference createPreference(String planName, String userEmail, String userName, String userCpf, String referenceType) throws MPException, MPApiException {
        
        MercadoPagoConfig.setAccessToken(mpAccessToken);
        PreferenceClient client = new PreferenceClient();

        SubscriptionPlan plan = SubscriptionPlan.getByName(planName);
        String planDescription;
        BigDecimal planPrice;
        
        switch (plan) {
            case SEMENTE:
                planDescription = "Plano Semente - 10 Mentorias/Mês";
                planPrice = SEMENTE_PRICE;
                break;
            case COLHEITA:
                planDescription = "Plano Colheita - 30 Mentorias/Mês";
                planPrice = COLHEITA_PRICE;
                break;
            case JARDINEIRO:
                planDescription = "Plano Jardineiro - Ilimitado";
                planPrice = JARDINEIRO_PRICE;
                break;
            default:
                throw new IllegalArgumentException("Plano inválido: " + planName);
        }
        
        // O formato agora é: TIPO-PLANO-EMAIL-UUID
        // Ex: "REG-SEMENTE-usuario@gmail.com-uuid"
        // Ex: "UPG-COLHEITA-usuario@gmail.com-uuid"
        String externalId = String.format("%s-%s-%s-%s",
                referenceType.toUpperCase(),
                planName.toUpperCase(),
                userEmail,
                UUID.randomUUID().toString());


        PreferenceItemRequest item = PreferenceItemRequest.builder()
            .title(planDescription)
            .quantity(1)
            .unitPrice(planPrice)
            .build();

        PreferencePayerRequest payer = PreferencePayerRequest.builder()
            .name(userName)
            .email(userEmail)
            .identification(IdentificationRequest.builder()
                .type("CPF")
                .number(userCpf)
                .build())
            .build();

        PreferenceRequest request = PreferenceRequest.builder()
            .items(Collections.singletonList(item))
            .externalReference(externalId) 
            .purpose("wallet_purchase") 
            .backUrls(PreferenceBackUrlsRequest.builder()
                .success(appBaseUrl + "/checkout/processing") 
                .pending(appBaseUrl + "/checkout/processing")
                // ****** INÍCIO DA CORREÇÃO (URL de Falha Dinâmica) ******
                .failure("REG".equalsIgnoreCase(referenceType) ?
                    appBaseUrl + "/checkout/cancel" :   // Falha no registro (volta p/ checkout)
                    appBaseUrl + "/upgrade/cancel")     // Falha no upgrade (volta p/ /my-plan)
                // ****** FIM DA CORREÇÃO ******
                .build())
            .notificationUrl(appBaseUrl + "/mercadopago/webhook") 
            .payer(payer)
            .build();
        
        return client.create(request);
    }
}