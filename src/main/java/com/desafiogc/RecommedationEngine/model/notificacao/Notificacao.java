package com.desafiogc.RecommedationEngine.model.notificacao;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoNotificacao tipo;

    @Column(name = "mensagem", nullable = false, columnDefinition = "text")
    private String mensagem;

    @Column(name = "canal")
    private String canal;

    @Column(name = "status")
    private String status;

    @Column(name = "criada_em")
    private LocalDateTime criadaEm;

    public Notificacao(Cliente cliente, TipoNotificacao tipo, String mensagem, String canal, String status) {
        this.cliente = cliente;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.canal = canal;
        this.status = status;
    }

    @PrePersist
    void definirDataCriacao() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
    }
}
