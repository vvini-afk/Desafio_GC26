package com.desafiogc.RecommedationEngine.api.catalogo;

import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoCatalogoDTO(
        Integer produtoId,
        String nomeProduto,
        String categoria,
        BigDecimal precoBase,
        BigDecimal precoMinimo,
        Integer promocaoId,
        BigDecimal valorDesconto,
        String tipoDesconto,
        boolean emPromocao,
        boolean jaComprado,
        boolean isCrossSell,
        List<String> motivos,
        List<String> pilares
) {
    public static ProdutoCatalogoDTO from(Produto produto, Promocao promocao) {
        return new ProdutoCatalogoDTO(
                produto.getId(),
                produto.getNome(),
                produto.getCategoria(),
                produto.getPrecoBase(),
                produto.getPrecoMinimo(),
                promocao == null ? null : promocao.getId(),
                promocao == null ? BigDecimal.ZERO.setScale(2) : promocao.getValorDesconto(),
                promocao == null ? "SEM_DESCONTO" : promocao.getTipoDesconto().name(),
                promocao != null,
                false,
                false,
                List.of(),
                List.of());
    }
}
