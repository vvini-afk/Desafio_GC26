package com.desafiogc.RecommedationEngine.model.recomendacao;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promocao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promocao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocao")
    private Integer id;

    @Column(nullable = false)
    private String nome;

    private Integer produtoId;

    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDesconto tipoDesconto;

    @Column(nullable = false)
    private BigDecimal valorDesconto;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    private Boolean ativa;

    public Promocao(
            String nome,
            Integer produtoId,
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
        this.inicio = inicio.atStartOfDay();
        this.fim = fim.atTime(23, 59, 59, 999_999_999);
        this.ativa = true;
    }

    public boolean vigente(LocalDate hoje) {
        return Boolean.TRUE.equals(ativa)
                && hoje != null
                && !hoje.isBefore(inicio.toLocalDate())
                && !hoje.isAfter(fim.toLocalDate());
    }

    public void desativar() {
        this.ativa = false;
    }
}
