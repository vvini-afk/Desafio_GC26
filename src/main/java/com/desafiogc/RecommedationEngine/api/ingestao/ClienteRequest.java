package com.desafiogc.RecommedationEngine.api.ingestao;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank String nome,
        String perfil,
        String cidade,
        String telefone,
        LocalDate ultimaCompra
) {
}
