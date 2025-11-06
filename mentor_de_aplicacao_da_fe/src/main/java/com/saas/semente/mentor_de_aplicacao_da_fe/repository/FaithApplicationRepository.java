package com.saas.semente.mentor_de_aplicacao_da_fe.repository;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.FaithApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaithApplicationRepository extends JpaRepository<FaithApplication, Long> {

    /**
     * NOVO: Busca todas as aplicações por ID do Usuário, ordenadas pela data de criação decrescente.
     * Spring Data JPA interpreta 'findByUserId' como: 
     * Encontre as aplicações onde o campo 'user' (na FaithApplication) tem o 'id' correspondente.
     */
    List<FaithApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // O método findAllByOrderByCreatedAtDesc() foi removido por razões de segurança, 
    // já que agora todos os dados devem ser filtrados por usuário.
}