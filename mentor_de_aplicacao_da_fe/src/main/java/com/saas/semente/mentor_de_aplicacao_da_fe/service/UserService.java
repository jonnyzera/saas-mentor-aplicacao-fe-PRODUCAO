package com.saas.semente.mentor_de_aplicacao_da_fe.service;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import com.saas.semente.mentor_de_aplicacao_da_fe.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Obtém o usuário logado a partir do Spring Security.
     * @return O objeto User.
     */
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName(); // Spring Security usa o email como username
        return userRepository.findByEmail(userEmail)
                             .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado no banco de dados."));
    }

    /**
     * Reseta o contador se for um novo mês.
     * @param user O usuário atual.
     * @return true se o contador foi resetado/checado, false se não foi.
     */
    @Transactional
    public void checkAndResetMonthlyPromptCount(User user) {
        // Verifica se é um novo mês desde o último uso
        if (user.getLastPromptMonthCheck().getMonth() != LocalDate.now().getMonth()) {
            // Se o mês da última checagem for diferente do mês atual, o contador é resetado.
            user.setMonthlyPromptCount(0);
            user.setLastPromptMonthCheck(LocalDateTime.now());
            userRepository.save(user); // Salva a alteração (reset)
        }
    }

    /**
     * Incrementa o contador de prompts e salva o usuário.
     * @param user O usuário atual.
     */
    @Transactional
    public void incrementPromptCount(User user) {
        user.setMonthlyPromptCount(user.getMonthlyPromptCount() + 1);
        userRepository.save(user);
    }
    
    /**
     * Salva ou atualiza um usuário.
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Busca um usuário pelo email (necessário para o fluxo de checkout)
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Verifica se o email já existe.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}