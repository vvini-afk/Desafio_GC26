package com.desafiogc.RecommedationEngine.api.ingestao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PedidoRequest(
        @NotNull Integer clienteId,
        @NotNull LocalDate dataCompra,
        @NotEmpty List<@Valid ItemRequest> itens
) {
    public record ItemRequest(
            @NotNull Integer produtoId,
            @Positive int quantidade,
            @NotNull @DecimalMin("0.00") BigDecimal valorUnitario
    ) {
    }
}
