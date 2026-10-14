package com.desafiogc.RecommedationEngine.api.ingestao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record PedidoRequest(
        @NotNull Long clienteId,
        @NotNull LocalDate dataCompra,
        @NotEmpty List<@Valid ItemRequest> itens
) {
    public record ItemRequest(@NotNull Long produtoId, @Positive int quantidade) {
    }
}
