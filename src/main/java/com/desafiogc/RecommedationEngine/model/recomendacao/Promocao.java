package com.desafiogc.RecommedationEngine.model.recomendacao;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promocao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promocao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private Long produtoId;

    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDesconto tipoDesconto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDesconto;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Column(nullable = false)
    private boolean ativa;

    public Promocao(
            String nome,
            Long produtoId,
            String categoria,
            TipoDesconto tipoDesconto,
            BigDecimal valorDesconto,
            LocalDate inicio,
            LocalDate fim)
    {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("O período da promoção é inválido.");
        }
        this.nome = nome;
        this.produtoId = produtoId;
        this.categoria = categoria;
        this.tipoDesconto = tipoDesconto;
        this.valorDesconto = valorDesconto;
        this.inicio = inicio;
        this.fim = fim;
        this.ativa = true;
    }

    public boolean vigente(LocalDate hoje) {
        return ativa
                && hoje != null
                && !hoje.isBefore(inicio)
                && !hoje.isAfter(fim);
    }

    public void desativar() {
        this.ativa = false;
    }
}
