package com.desafiogc.RecommedationEngine.api.cliente;

import java.util.List;

public record VendedorPainelDTO(
        Integer idHierarquia,
        Integer codRepresentante,
        String nomeVendedor,
        int totalClientes,
        long clientesEmQuebra,
        long totalCidades,
        String ufsAtendidas,
        List<ClienteResumoDTO> clientes
) {
    public VendedorPainelDTO {
        clientes = clientes == null ? List.of() : List.copyOf(clientes);
    }
}

