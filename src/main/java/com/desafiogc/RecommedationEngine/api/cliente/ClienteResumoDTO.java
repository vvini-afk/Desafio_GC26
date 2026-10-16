package com.desafiogc.RecommedationEngine.api.cliente;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;

import java.time.LocalDate;

public record ClienteResumoDTO(
        Long id,
        String nome,
        String perfil,
        String cidade,
        LocalDate ultimaCompra
) {
    public static ClienteResumoDTO from(Cliente cliente) {
        return new ClienteResumoDTO(
                cliente.getId(), cliente.getNome(), cliente.getPerfil(), cliente.getCidade(),
                cliente.getUltimaCompra() == null ? null : cliente.getUltimaCompra().toLocalDate()
        );
    }
}
