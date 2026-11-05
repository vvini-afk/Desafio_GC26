package com.desafiogc.RecommedationEngine.api.recomendacao;

import java.util.List;

public record LoteRecomendacoesDTO(
        int totalClientesConsultados,
        long clientesComRecomendacoes,
        List<RecomendacoesClienteDTO> clientes
) {
    public LoteRecomendacoesDTO {
        clientes = clientes == null ? List.of() : List.copyOf(clientes);
    }
}
