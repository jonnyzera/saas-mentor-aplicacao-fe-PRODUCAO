package com.saas.semente.mentor_de_aplicacao_da_fe.repository;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Método essencial para o Spring Security: buscar o usuário pelo email (username)
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}