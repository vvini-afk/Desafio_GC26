package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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

    @Transactional(readOnly = true)
    public List<Recomendacao> recomendar(Long clienteId) {
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        List<Pedido> historico = pedidoRepository.findByCliente_Id(clienteId);
        Map<String, Long> comprasPorCategoria = historico.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .collect(Collectors.groupingBy(item -> item.getProduto().getCategoria().toLowerCase(Locale.ROOT), Collectors.counting()));
        Set<Long> produtosComprados = historico.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .map(item -> item.getProduto().getId())
                .collect(Collectors.toSet());
        List<Promocao> promocoesVigentes = promocaoRepository.findAll().stream()
                .filter(promocao -> promocao.vigente(LocalDate.now()))
                .toList();

        return produtoRepository.findByAtivoTrueOrderByIdAsc().stream()
                .map(produto -> pontuar(produto, comprasPorCategoria, produtosComprados, promocoesVigentes))
                .sorted(Comparator.comparingDouble(Recomendacao::score).reversed()
                        .thenComparing(recomendacao -> recomendacao.produto().getId()))
                .toList();
    }

    private Recomendacao pontuar(Produto produto, Map<String, Long> comprasPorCategoria,
                                    Set<Long> produtosComprados, List<Promocao> promocoes) {
        double score = SCORE_BASE;
        List<String> motivos = new ArrayList<>();
        long frequenciaCategoria = comprasPorCategoria.getOrDefault(produto.getCategoria().toLowerCase(Locale.ROOT), 0L);
        if (frequenciaCategoria > 0) {
            int pontos = (int) Math.min(LIMITE_PONTOS_CATEGORIA,
                    frequenciaCategoria * PONTOS_POR_ITEM_DA_CATEGORIA);
            score += pontos;
            motivos.add("Categoria " + produto.getCategoria() + " presente no histórico (" + frequenciaCategoria + " item(ns), +" + pontos + ")");
        }
        boolean complementar = produto.getComplementaresIds().stream().anyMatch(produtosComprados::contains);
        if (complementar) {
            score += PONTOS_COMPLEMENTAR;
            motivos.add("Complementar a um produto já comprado (+20)");
        }
        Promocao promocao = promocoes.stream()
                .filter(p -> produto.getId().equals(p.getProdutoId())
                        || (p.getCategoria() != null && produto.getCategoria().equalsIgnoreCase(p.getCategoria())))
                .findFirst().orElse(null);
        if (promocao != null) {
            score += PONTOS_PROMOCAO;
            motivos.add("Promoção vigente: " + promocao.getNome() + " (+15)");
        }
        if (motivos.isEmpty()) {
            motivos.add("Produto ativo; sem sinais de preferência ou promoção (+10)");
        }
        return new Recomendacao(produto, score, motivos, promocao);
    }
}
