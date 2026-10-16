package com.desafiogc.RecommedationEngine.model.produto;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "cod_familia", nullable = false)
    private String categoria;

    @Column(name = "preco_base")
    private BigDecimal precoBase;

    @Column(name = "preco_minimo")
    private BigDecimal precoMinimo;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection
    @CollectionTable(
            name = "produto_complementar",
            joinColumns = @JoinColumn(name = "produto_id", referencedColumnName = "id_item")
    )
    @Column(name = "complementar_id", nullable = false)
    private List<Long> complementaresIds = new ArrayList<>();

    public Produto(String nome, String categoria) {
        this.nome = nome;
        this.categoria = categoria;
    }

    public Produto(String nome, String categoria, BigDecimal precoBase, BigDecimal precoMinimo) {
        this(nome, categoria);
        this.precoBase = precoBase;
        this.precoMinimo = precoMinimo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void adicionarComplementar(Long produtoId) {
        if (produtoId == null || produtoId.equals(this.id)) {
            throw new IllegalArgumentException(
                    "O produto complementar precisa ter um ID válido e ser diferente deste produto."
            );
        }

        if (!complementaresIds.contains(produtoId)) {
            complementaresIds.add(produtoId);
        }
    }
}
