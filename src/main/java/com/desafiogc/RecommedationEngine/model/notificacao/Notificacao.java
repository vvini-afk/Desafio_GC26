package com.desafiogc.RecommedationEngine.model.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Integer id;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    private StatusEnvio status;

    @Column(name = "criada_em")
    private LocalDateTime criadaEm;

    public Notificacao(
            Integer clienteId,
            TipoNotificacao tipo,
            String mensagem,
            Canal canal,
            StatusEnvio status,
            LocalDateTime criadaEm
    ) {
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.canal = canal;
        this.status = status;
        this.criadaEm = criadaEm;
    }
}
