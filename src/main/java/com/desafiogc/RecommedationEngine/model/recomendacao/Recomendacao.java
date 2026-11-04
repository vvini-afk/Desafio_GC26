package com.desafiogc.RecommedationEngine.model.recomendacao;

import com.desafiogc.RecommedationEngine.model.produto.Produto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record Recomendacao(
        Produto produto,
        double score,
        List<String> motivos,
        Promocao promocao,
        boolean jaComprado,
        List<PilarRecomendacao> pilares,
        boolean recomendavel
) {
    public Recomendacao(Produto produto, double score, List<String> motivos, Promocao promocao,
                        boolean jaComprado, List<PilarRecomendacao> pilares) {
        this(produto, score, motivos, promocao, jaComprado, pilares, true);
    }

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

    public Recomendacao aplicarMultiplicadorRegional(String regiao, BigDecimal multiplicador) {
        if (multiplicador == null || multiplicador.compareTo(BigDecimal.ONE) == 0) {
            return this;
        }

        double fator = multiplicador.doubleValue();
        List<String> motivosAtualizados = new ArrayList<>(motivos);
        motivosAtualizados.add(String.format(Locale.ROOT,
                "Multiplicador regional %s (x%s) aplicado",
                regiao == null || regiao.isBlank() ? "não informada" : regiao,
                multiplicador.stripTrailingZeros().toPlainString()));
        return new Recomendacao(produto, score * fator, motivosAtualizados, promocao,
                jaComprado, pilares, recomendavel);
    }
}
