package com.desafiogc.RecommedationEngine.api.ingestao;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoRequest(
        @NotBlank String nome,
        @NotBlank String categoria,
        List<Long> produtosComplementaresIds,
        BigDecimal precoBase,
        BigDecimal precoMinimo
) {
    public ProdutoRequest {
        produtosComplementaresIds = produtosComplementaresIds == null ? List.of() : List.copyOf(produtosComplementaresIds);
    }
}
