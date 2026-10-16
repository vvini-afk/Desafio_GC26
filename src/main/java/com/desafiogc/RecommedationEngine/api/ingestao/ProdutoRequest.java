package com.desafiogc.RecommedationEngine.api.ingestao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoRequest(
        @NotBlank String nome,
        @NotBlank String categoria,
        List<Integer> produtosComplementaresIds,
        @NotNull @DecimalMin("0.00") BigDecimal precoBase,
        @NotNull @DecimalMin("0.00") BigDecimal precoMinimo
) {
    public ProdutoRequest {
        produtosComplementaresIds = produtosComplementaresIds == null ? List.of() : List.copyOf(produtosComplementaresIds);
    }
}
