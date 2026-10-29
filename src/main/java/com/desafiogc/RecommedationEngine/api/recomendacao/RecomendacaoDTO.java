package com.desafiogc.RecommedationEngine.api.recomendacao;

import com.desafiogc.RecommedationEngine.model.recomendacao.Recomendacao;
import com.desafiogc.RecommedationEngine.model.recomendacao.PilarRecomendacao;
import com.desafiogc.RecommedationEngine.model.recomendacao.TipoDesconto;

import java.math.BigDecimal;
import java.util.List;

public record RecomendacaoDTO(
        Integer produtoId,
        String nomeProduto,
        String categoria,
        double score,
        List<String> motivos,
        Integer promocaoId,
        BigDecimal precoBase,
        BigDecimal precoMinimo,
        BigDecimal valorDesconto,
        TipoDesconto tipoDesconto,
        boolean jaComprado,
        boolean emPromocao,
        boolean isCrossSell,
        List<PilarRecomendacao> pilares
) {
    public RecomendacaoDTO {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
        pilares = pilares == null ? List.of() : List.copyOf(pilares);
    }

    public static RecomendacaoDTO from(Recomendacao recomendacao) {
        return new RecomendacaoDTO(
                recomendacao.produto().getId(),
                recomendacao.produto().getNome(),
                recomendacao.produto().getCategoria(),
                recomendacao.score(),
                recomendacao.motivos(),
                recomendacao.promocao() == null ? null : recomendacao.promocao().getId(),
                recomendacao.produto().getPrecoBase(),
                recomendacao.produto().getPrecoMinimo(),
                recomendacao.promocao() == null ? null : recomendacao.promocao().getValorDesconto(),
                recomendacao.promocao() == null ? null : recomendacao.promocao().getTipoDesconto(),
                recomendacao.jaComprado(),
                recomendacao.emPromocao(),
                recomendacao.isCrossSell(),
                recomendacao.pilares()
        );
    }
}
