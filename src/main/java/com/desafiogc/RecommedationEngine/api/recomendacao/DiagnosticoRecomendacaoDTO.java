package com.desafiogc.RecommedationEngine.api.recomendacao;

import java.math.BigDecimal;
import java.util.List;

public record DiagnosticoRecomendacaoDTO(
        Integer clienteId,
        String cidadeConsultadaParaClima,
        String cidadeRegional,
        String regiao,
        BigDecimal multiplicadorConfigurado,
        BigDecimal multiplicadorAplicado,
        int totalItensRetornados,
        long recomprasDiretas,
        long itensIneditos,
        long crossSellsIneditos,
        List<RecomendacaoDTO> recomendacoes
) {
    public DiagnosticoRecomendacaoDTO {
        recomendacoes = recomendacoes == null ? List.of() : List.copyOf(recomendacoes);
    }
}
