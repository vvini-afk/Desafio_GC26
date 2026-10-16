package com.desafiogc.RecommedationEngine.api.ingestao;

import com.desafiogc.RecommedationEngine.model.recomendacao.TipoDesconto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromocaoRequest(
        @NotBlank String nome,
        Integer produtoId,
        String categoria,
        @NotNull TipoDesconto tipoDesconto,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valorDesconto,
        @NotNull LocalDate inicio,
        @NotNull LocalDate fim
) {
}
