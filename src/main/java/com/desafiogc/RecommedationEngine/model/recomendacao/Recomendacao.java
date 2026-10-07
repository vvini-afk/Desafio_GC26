package com.desafiogc.RecommedationEngine.model.recomendacao;

import com.desafiogc.RecommedationEngine.model.produto.Produto;

import java.util.List;
import java.util.Objects;

public record Recomendacao(
        Produto produto,
        double score,
        List<String> motivos,
        Promocao promocao
) {
    public Recomendacao {
        Objects.requireNonNull(produto, "produto não pode ser nulo");
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
    }
}
