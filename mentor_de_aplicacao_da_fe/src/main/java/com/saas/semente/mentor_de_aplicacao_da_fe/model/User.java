package com.saas.semente.mentor_de_aplicacao_da_fe.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections; 

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String identificationNumber;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // NOVO CAMPO 1: Plano de Assinatura
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.SEMENTE;

    // NOVO CAMPO 2: Contador de prompts
    private int monthlyPromptCount = 0;

    // NOVO CAMPO 3: Data da última checagem/reset
    private LocalDateTime lastPromptMonthCheck = LocalDateTime.now();

    // Campo isEnabled Adicionado (Usuário inativo por padrão)
    @Column(nullable = false)
    private boolean isEnabled = false; 


    // 1. Método Obrigatório: Define as Autoridades (Roles)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    // 2. Método Corrigido/Obrigatório: Define o Nome de Usuário (usamos o email)
    @Override
    public String getUsername() {
        return this.email;
    }

    // 3. Métodos Obrigatórios: Status da Conta
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // O MÉTODO AGORA FUNCIONA: Retorna o valor do campo isEnabled
    @Override
    public boolean isEnabled() {
        return this.isEnabled; 
    }
}