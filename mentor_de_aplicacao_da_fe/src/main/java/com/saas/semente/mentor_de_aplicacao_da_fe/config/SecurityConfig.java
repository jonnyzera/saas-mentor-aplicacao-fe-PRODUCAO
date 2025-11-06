package com.saas.semente.mentor_de_aplicacao_da_fe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Desabilitar CSRF (Forma moderna)
            .csrf(csrf -> csrf
                // ****** INÍCIO DA CORREÇÃO ******
                .ignoringRequestMatchers(
                    "/mercadopago/webhook", // Ignora o Webhook
                    "/delete/**"            // Ignora TODAS as rotas que começam com /delete/
                )
                // ****** FIM DA CORREÇÃO ******
            )
            
            // 2. Configurar autorizações
            .authorizeHttpRequests(auth -> auth
                // 2a. Rotas PÚBLICAS (não precisam de login)
                // Usamos os métodos estáticos HttpMethod.POST e HttpMethod.GET
                .requestMatchers(
                    // Páginas de entrada e registro (GET)
                    "/lading-page-saas",
                    "/login",
                    "/register",
                    "/checkout",
                    "/checkout/processing",
                    
                    // Rota de cancelamento de REGISTRO
                    "/checkout/cancel", 
                    "/checkout/failure",
                    
                    // Rota de cancelamento de UPGRADE
                    "/upgrade/cancel",
                    
                    // Recursos estáticos
                    "/css/**",
                    "/js/**",
                    "/webjars/**",
                    "/favicon.ico"
                ).permitAll()
                
                // Rotas específicas de POST que devem ser públicas
                .requestMatchers(HttpMethod.POST, "/checkout/process").permitAll()
                .requestMatchers(HttpMethod.POST, "/mercadopago/webhook").permitAll()
                
                // 2b. TODAS as outras rotas exigem autenticação
                .anyRequest().authenticated()
            )
            
            // 3. Configurar o formulário de Login
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true) 
                .permitAll()
            )
            
            // 4. Configurar o Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}