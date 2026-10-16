package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.model.recomendacao.Recomendacao;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.repository.PedidoRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import com.desafiogc.RecommedationEngine.repository.PromocaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorRecomendacaoService {
    private static final double SCORE_BASE = 10.0;
    private static final int PONTOS_POR_ITEM_DA_CATEGORIA = 5;
    private static final int LIMITE_PONTOS_CATEGORIA = 25;
    private static final double PONTOS_COMPLEMENTAR = 20.0;
    private static final double PONTOS_PROMOCAO = 15.0;

    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final PromocaoRepository promocaoRepository;
    private final ContextoRecomendacaoService contextoRecomendacaoService;

    public List<Recomendacao> recomendar(Integer clienteId, String cidadeOverride) {
        var cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        String cidade = cidadeOverride == null || cidadeOverride.isBlank()
                ? cliente.getCidade()
                : cidadeOverride.trim();
        ContextoRecomendacao contexto = contextoRecomendacaoService.consultar(cidade);

        List<Pedido> historico = pedidoRepository.findByCliente_Id(clienteId);
        Map<String, Long> comprasPorCategoria = historico.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .collect(Collectors.groupingBy(item -> normalizarCategoria(item.getProduto().getCategoria()), Collectors.counting()));
        Set<Integer> produtosComprados = historico.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .map(item -> item.getProduto().getId())
                .collect(Collectors.toSet());
        List<Promocao> promocoesVigentes = promocaoRepository.findAll().stream()
                .filter(promocao -> promocao.vigente(LocalDate.now()))
                .toList();

        return produtoRepository.findByAtivoTrueOrderByIdAsc().stream()
                .map(produto -> pontuar(produto, comprasPorCategoria, produtosComprados, promocoesVigentes, contexto))
                .sorted(Comparator.comparingDouble(Recomendacao::score).reversed()
                        .thenComparing(recomendacao -> recomendacao.produto().getId()))
                .toList();
    }

    private Recomendacao pontuar(Produto produto, Map<String, Long> comprasPorCategoria,
                                 Set<Integer> produtosComprados, List<Promocao> promocoes,
                                 ContextoRecomendacao contexto) {
        double score = SCORE_BASE;
        List<String> motivos = new ArrayList<>();
        boolean temSinalDeRecomendacao = false;
        long frequenciaCategoria = comprasPorCategoria.getOrDefault(normalizarCategoria(produto.getCategoria()), 0L);
        if (frequenciaCategoria > 0) {
            int pontos = (int) Math.min(LIMITE_PONTOS_CATEGORIA,
                    frequenciaCategoria * PONTOS_POR_ITEM_DA_CATEGORIA);
            score += pontos;
            temSinalDeRecomendacao = true;
            motivos.add("Categoria " + produto.getCategoria() + " presente no histórico (" + frequenciaCategoria + " item(ns), +" + pontos + ")");
        }
        boolean complementar = produto.getComplementaresIds().stream().anyMatch(produtosComprados::contains);
        if (complementar) {
            score += PONTOS_COMPLEMENTAR;
            temSinalDeRecomendacao = true;
            motivos.add("Complementar a um produto já comprado (+20)");
        }
        Promocao promocao = promocoes.stream()
                .filter(p -> p.getProdutoId() != null
                        ? produto.getId().equals(p.getProdutoId())
                        : p.getCategoria() != null
                                && normalizarCategoria(produto.getCategoria()).equals(normalizarCategoria(p.getCategoria())))
                .sorted(Comparator.comparing((Promocao p) -> p.getProdutoId() == null)
                        .thenComparing(Promocao::getId))
                .findFirst().orElse(null);
        if (promocao != null) {
            score += PONTOS_PROMOCAO;
            temSinalDeRecomendacao = true;
            motivos.add("Promoção vigente: " + promocao.getNome() + " (+15)");
        }
        PontuacaoContextual pontuacaoContextual = contextoRecomendacaoService.pontuar(produto, contexto);
        score += pontuacaoContextual.pontos();
        motivos.addAll(pontuacaoContextual.motivos());
        temSinalDeRecomendacao |= pontuacaoContextual.pontos() > 0;
        if (!temSinalDeRecomendacao) {
            motivos.add("Produto ativo; sem fatores de preferência ou contexto aplicáveis (+10)");
        }
        return new Recomendacao(produto, score, motivos, promocao);
    }

    private static String normalizarCategoria(String categoria) {
        return Normalizer.normalize(categoria == null ? "" : categoria, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
