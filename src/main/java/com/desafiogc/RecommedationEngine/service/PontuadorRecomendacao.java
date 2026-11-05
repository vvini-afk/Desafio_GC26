package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.PilarRecomendacao;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.model.recomendacao.Recomendacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PontuadorRecomendacao {
    private static final double SCORE_BASE = 10.0;
    private static final int PONTOS_RECOMPRA_DIRETA = 5;
    private static final int PONTOS_POR_ITEM_DA_CATEGORIA = 3;
    private static final int LIMITE_PONTOS_CATEGORIA = 10;
    private static final double PONTOS_COMPLEMENTAR = 20.0;
    private static final double PONTOS_PROMOCAO = 15.0;

    private final PontuadorContextual pontuadorContextual;

    public Recomendacao pontuar(Produto produto, Map<String, Long> comprasPorCategoria,
                                Set<Integer> produtosComprados, List<Promocao> promocoes,
                                ContextoRecomendacao contexto) {
        double score = SCORE_BASE;
        List<String> motivos = new ArrayList<>();
        List<PilarRecomendacao> pilares = new ArrayList<>();
        boolean temSinalDeRecomendacao = false;
        boolean jaComprado = produtosComprados.contains(produto.getId());
        long frequenciaCategoria = comprasPorCategoria.getOrDefault(
                NormalizadorCategoria.normalizar(produto.getCategoria()), 0L);
        if (jaComprado) {
            score += PONTOS_RECOMPRA_DIRETA;
            temSinalDeRecomendacao = true;
            pilares.add(PilarRecomendacao.HISTORICO);
            motivos.add("Recompra direta: produto já consta no histórico (+"
                    + PONTOS_RECOMPRA_DIRETA + ")");
        } else if (frequenciaCategoria > 0) {
            int pontos = (int) Math.min(LIMITE_PONTOS_CATEGORIA,
                    frequenciaCategoria * PONTOS_POR_ITEM_DA_CATEGORIA);
            score += pontos;
            temSinalDeRecomendacao = true;
            pilares.add(PilarRecomendacao.HISTORICO);
            motivos.add("Categoria " + produto.getCategoria() + " presente no histórico ("
                    + frequenciaCategoria + " item(ns), +" + pontos + ")");
        }

        boolean complementar = !jaComprado
                && produto.getComplementaresIds().stream().anyMatch(produtosComprados::contains);
        if (complementar) {
            score += PONTOS_COMPLEMENTAR;
            temSinalDeRecomendacao = true;
            pilares.add(PilarRecomendacao.CROSS_SELL);
            motivos.add("Complementar a um produto já comprado (+20)");
        }

        Promocao promocao = promocoes.stream()
                .filter(p -> p.getProdutoId() != null
                        ? produto.getId().equals(p.getProdutoId())
                        : p.getCategoria() != null && NormalizadorCategoria.normalizar(produto.getCategoria())
                        .equals(NormalizadorCategoria.normalizar(p.getCategoria())))
                .sorted(Comparator.comparing((Promocao p) -> p.getProdutoId() == null)
                        .thenComparing(Promocao::getId))
                .findFirst().orElse(null);
        if (promocao != null) {
            score += PONTOS_PROMOCAO;
            temSinalDeRecomendacao = true;
            pilares.add(PilarRecomendacao.PROMOCAO);
            motivos.add("Promoção vigente: " + promocao.getNome() + " (+15)");
        }

        PontuacaoContextual pontuacaoContextual = pontuadorContextual.pontuar(produto, contexto);
        score += pontuacaoContextual.pontos();
        motivos.addAll(pontuacaoContextual.motivos());
        temSinalDeRecomendacao |= pontuacaoContextual.pontos() > 0;
        if (!temSinalDeRecomendacao) {
            motivos.add("Produto ativo; sem fatores de preferência ou contexto aplicáveis (+10)");
        }
        return new Recomendacao(produto, score, motivos, promocao,
                jaComprado, pilares, temSinalDeRecomendacao);
    }
}
