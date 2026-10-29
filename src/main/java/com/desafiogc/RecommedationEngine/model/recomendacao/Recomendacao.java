package com.desafiogc.RecommedationEngine.model.recomendacao;

import com.desafiogc.RecommedationEngine.model.produto.Produto;

import java.util.List;
import java.util.Objects;

public record Recomendacao(
        Produto produto,
        double score,
        List<String> motivos,
        Promocao promocao,
        boolean jaComprado,
        List<PilarRecomendacao> pilares
) {
    public Recomendacao {
        Objects.requireNonNull(produto, "produto não pode ser nulo");
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
        pilares = pilares == null ? List.of() : List.copyOf(pilares);
    }

    public boolean emPromocao() {
        return promocao != null;
    }

    public boolean isCrossSell() {
        return pilares.contains(PilarRecomendacao.CROSS_SELL);
    }
}
