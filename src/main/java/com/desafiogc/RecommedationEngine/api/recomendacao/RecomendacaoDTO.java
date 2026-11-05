package com.desafiogc.RecommedationEngine.api.recomendacao;

import com.desafiogc.RecommedationEngine.model.recomendacao.Recomendacao;
import com.desafiogc.RecommedationEngine.model.recomendacao.PilarRecomendacao;

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
        String tipoDesconto,
        boolean jaComprado,
        boolean emPromocao,
        boolean isCrossSell,
        List<PilarRecomendacao> pilares
) {
    private static final BigDecimal VALOR_SEM_DESCONTO = BigDecimal.ZERO.setScale(2);

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
                recomendacao.promocao() == null ? VALOR_SEM_DESCONTO : recomendacao.promocao().getValorDesconto(),
                recomendacao.promocao() == null ? "SEM_DESCONTO" : recomendacao.promocao().getTipoDesconto().name(),
                recomendacao.jaComprado(),
                recomendacao.emPromocao(),
                recomendacao.isCrossSell(),
                recomendacao.pilares()
        );
    }
}
