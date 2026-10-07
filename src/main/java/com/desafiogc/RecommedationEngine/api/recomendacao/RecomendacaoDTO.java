package com.desafiogc.RecommedationEngine.api.recomendacao;

import java.util.List;

public record RecomendacaoDTO(
        Long produtoId,
        String nomeProduto,
        String categoria,
        double score,
        List<String> motivos,
        Long promocaoId
) {
    public RecomendacaoDTO {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
    }
}
