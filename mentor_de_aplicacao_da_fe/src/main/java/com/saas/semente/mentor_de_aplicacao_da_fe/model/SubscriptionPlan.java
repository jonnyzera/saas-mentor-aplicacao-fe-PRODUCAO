package com.saas.semente.mentor_de_aplicacao_da_fe.model;

public enum SubscriptionPlan {
    SEMENTE(10),       // 10 Mentorias/Mês
    COLHEITA(30),      // 30 Mentorias/Mês (Plano Popular)
    JARDINEIRO(Integer.MAX_VALUE); // Mentorias Ilimitadas

    private final int monthlyLimit;

    SubscriptionPlan(int monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public static SubscriptionPlan getByName(String name) {
        try {
            return SubscriptionPlan.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Plano de assinatura inválido: " + name);
        }
    }
}
