package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.repository.PedidoRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CrossSellService {

    private static final int LIMITE_COMPLEMENTARES = 3;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public Resultado recalcular() {
        List<PedidoRepository.ProdutoPedidoPar> itens = pedidoRepository.listarParesDeProdutos();
        Map<Integer, Set<Integer>> produtosPorPedido = new HashMap<>();
        for (var item : itens) {
            produtosPorPedido.computeIfAbsent(item.getPedidoId(), ignorado -> new HashSet<>())
                    .add(item.getProdutoId());
        }

        Map<Integer, Long> pedidosPorProduto = new HashMap<>();
        Map<ParProdutos, Long> frequenciaDosPares = new HashMap<>();
        for (Set<Integer> produtosNoPedido : produtosPorPedido.values()) {
            for (Integer produtoId : produtosNoPedido) {
                pedidosPorProduto.merge(produtoId, 1L, Long::sum);
            }

            List<Integer> produtos = new ArrayList<>(produtosNoPedido);
            for (int i = 0; i < produtos.size(); i++) {
                for (int j = 0; j < produtos.size(); j++) {
                    if (i != j) {
                        frequenciaDosPares.merge(
                                new ParProdutos(produtos.get(i), produtos.get(j)), 1L, Long::sum);
                    }
                }
            }
        }

        Map<Integer, Produto> catalogo = new HashMap<>();
        produtoRepository.findAll().forEach(produto -> catalogo.put(produto.getId(), produto));

        Map<Integer, List<Integer>> novosComplementares = new HashMap<>();
        int paresSelecionados = 0;
        for (Produto produto : catalogo.values()) {
            long pedidosDoProduto = pedidosPorProduto.getOrDefault(produto.getId(), 0L);
            if (pedidosDoProduto == 0) {
                continue;
            }

            List<ParPontuado> melhores = frequenciaDosPares.entrySet().stream()
                    .filter(entry -> entry.getKey().produtoOrigem().equals(produto.getId()))
                    .filter(entry -> {
                        Produto complementar = catalogo.get(entry.getKey().produtoDestino());
                        return complementar != null && Boolean.TRUE.equals(complementar.getAtivo());
                    })
                    .map(entry -> new ParPontuado(
                            entry.getKey().produtoDestino(),
                            entry.getValue(),
                            (double) entry.getValue() / pedidosDoProduto))
                    .sorted(Comparator.comparingDouble(ParPontuado::confianca).reversed()
                            .thenComparing(Comparator.comparingLong(ParPontuado::frequencia).reversed())
                            .thenComparing(ParPontuado::produtoDestino))
                    .limit(LIMITE_COMPLEMENTARES)
                    .toList();

            for (ParPontuado par : melhores) {
                paresSelecionados++;
                novosComplementares
                        .computeIfAbsent(par.produtoDestino(), ignorado -> new ArrayList<>())
                        .add(produto.getId());
            }
        }

        for (Produto produto : catalogo.values()) {
            List<Integer> desejados = novosComplementares.getOrDefault(produto.getId(), List.of());
            List<Integer> atuais = produto.getComplementaresIds() == null
                    ? List.of()
                    : produto.getComplementaresIds();
            boolean iguais = atuais.size() == desejados.size() && atuais.containsAll(desejados);
            if (!iguais) {
                produto.limparComplementares();
                for (Integer complementarId : desejados) {
                    produto.adicionarComplementar(complementarId);
                }
            }
        }

        return new Resultado(catalogo.size(), paresSelecionados, paresSelecionados);
    }

    private record ParProdutos(Integer produtoOrigem, Integer produtoDestino) { }

    private record ParPontuado(Integer produtoDestino, long frequencia, double confianca) { }

    public record Resultado(int produtosAnalisados, int paresSelecionados, int associacoesPersistidas) { }
}
