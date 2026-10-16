package com.desafiogc.RecommedationEngine.api.recomendacao;

import com.desafiogc.RecommedationEngine.model.recomendacao.Recomendacao;

import java.util.List;

public record RecomendacaoDTO(
        Integer produtoId,
        String nomeProduto,
        String categoria,
        double score,
        List<String> motivos,
        Integer promocaoId
) {
    public RecomendacaoDTO {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
    }

    public static RecomendacaoDTO from(Recomendacao recomendacao) {
        return new RecomendacaoDTO(
                recomendacao.produto().getId(),
                recomendacao.produto().getNome(),
                recomendacao.produto().getCategoria(),
                recomendacao.score(),
                recomendacao.motivos(),
                recomendacao.promocao() == null ? null : recomendacao.promocao().getId()
        );
    }
}
