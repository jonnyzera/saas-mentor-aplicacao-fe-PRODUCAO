package com.saas.semente.mentor_de_aplicacao_da_fe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob; 
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class FaithApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) 
    private User user; 

    // Campos preenchidos pelo usuário
    @Lob // Garante que o texto seja longo (TEXT)
    private String userChallenge;

    // Campos gerados pela IA (Mentor Premium)
    private String identifiedTheme;

    @Lob // Garante que o texto seja longo (TEXT)
    private String versiculoBussola;

    @Lob // Garante que o texto seja longo (TEXT)
    private String reflexaoAplicada;

    // ****** INÍCIO DA CORREÇÃO ******
    @Lob // Garante que o texto seja longo (TEXT)
    private String conselhosPraticos; // NOVO CAMPO
    // ****** FIM DA CORREÇÃO ******

    private String referenciasCruzadas;

    @Lob // Garante que o texto seja longo (TEXT)
    private String oracaoSemente;

    @CreationTimestamp
    private LocalDateTime createdAt;
}