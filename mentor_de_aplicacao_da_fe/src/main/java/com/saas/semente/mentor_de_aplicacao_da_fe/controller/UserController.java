package com.saas.semente.mentor_de_aplicacao_da_fe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    /**
     * Rota GET para exibir a página de login.
     */
    @GetMapping("/login")
    public String login() {
        return "login"; // Retorna o template login.html
    }

    /**
     * Rota GET para exibir o formulário de registro.
     */
    @GetMapping("/register")
    public String registerForm() {
        return "redirect:/lading-page-saas"; // Redireciona para a landing page (fluxo pago)
    }

    // NOVO MÉTODO: Mapeamento explícito para a Landing Page
    @GetMapping("/lading-page-saas")
    public String ladingPage() {
        return "lading-page-saas"; // Retorna o template lading-page-saas.html
    }

    /**
     * Rota POST para processar a submissão do formulário de registro.
     */
    @PostMapping("/register")
    public String registerUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes ra,
            Model model) {

        // Lógica de Bloqueio para o Modelo Exclusivamente Pago

        ra.addFlashAttribute("errorMessage", "A criação de conta é feita exclusivamente através da escolha de um plano pago. Por favor, escolha seu plano para começar.");

        return "redirect:/lading-page-saas";
    }
}