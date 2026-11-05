package com.desafiogc.RecommedationEngine.api.recomendacao;

import java.util.List;

public record RecomendacoesClienteDTO(
        Integer clienteId,
        List<RecomendacaoDTO> recomendacoes
) {
    public RecomendacoesClienteDTO {
        recomendacoes = recomendacoes == null ? List.of() : List.copyOf(recomendacoes);
    }
}
