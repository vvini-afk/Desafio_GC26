package com.desafiogc.RecommedationEngine.model.produto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "estoque_erp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EstoqueErp {

    @Id
    @Column(name = "produto_id")
    private Integer produtoId;

    @Column(name = "quantidade_disponivel", nullable = false)
    private Integer quantidadeDisponivel;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;
}
