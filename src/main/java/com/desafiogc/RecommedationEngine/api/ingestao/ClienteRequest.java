package com.desafiogc.RecommedationEngine.api.ingestao;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ClienteRequest(
        String cnpj,
        @NotBlank String nome,
        String perfil,
        String subsegmento,
        String cep,
        Integer hierarquiaId,
        Integer cidadeId,
        String cidade,
        String telefone,
        LocalDate ultimaCompra
) {
}
