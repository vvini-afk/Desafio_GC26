package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.PilarRecomendacao;
import com.desafiogc.RecommedationEngine.repository.PedidoRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CrossSellServiceTest {

    @Test
    void persisteAssociacoesNoSentidoQueOPontuadorConsulta() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        ProdutoRepository produtos = mock(ProdutoRepository.class);
        Produto produtoComprado = produto(1);
        Produto candidato = produto(2);
        var itemComprado = itemPedido(10, 1);
        var itemComplementar = itemPedido(10, 2);
        when(pedidos.listarParesDeProdutos()).thenReturn(List.of(itemComprado, itemComplementar));
        when(produtos.findAll()).thenReturn(List.of(produtoComprado, candidato));

        CrossSellService.Resultado resultado = new CrossSellService(pedidos, produtos).recalcular();

        assertEquals(2, resultado.paresSelecionados());
        assertEquals(2, resultado.associacoesPersistidas());
        verify(candidato).adicionarComplementar(1);
        verify(produtoComprado).adicionarComplementar(2);
    }

    @Test
    void recomendadorAplicaVintePontosQuandoProdutoCompradoEstaAssociado() {
        PontuadorContextual contextual = mock(PontuadorContextual.class);
        when(contextual.pontuar(any(), any())).thenReturn(new PontuacaoContextual(0, List.of()));
        PontuadorRecomendacao pontuador = new PontuadorRecomendacao(contextual);
        Produto candidato = produto(2);
        when(candidato.getComplementaresIds()).thenReturn(List.of(1));

        var recomendacao = pontuador.pontuar(
                candidato,
                Map.of(),
                Set.of(1),
                List.of(),
                new ContextoRecomendacao(null, List.of(), List.of()));

        assertEquals(30.0, recomendacao.score());
        assertTrue(recomendacao.pilares().contains(PilarRecomendacao.CROSS_SELL));
        assertTrue(recomendacao.motivos().contains("Complementar a um produto já comprado (+20)"));
    }

    private static Produto produto(Integer id) {
        Produto produto = mock(Produto.class);
        List<Integer> complementares = new ArrayList<>();
        when(produto.getId()).thenReturn(id);
        when(produto.getAtivo()).thenReturn(true);
        when(produto.getComplementaresIds()).thenReturn(complementares);
        doAnswer(invocation -> {
            complementares.clear();
            return null;
        }).when(produto).limparComplementares();
        doAnswer(invocation -> {
            complementares.add(invocation.getArgument(0));
            return null;
        }).when(produto).adicionarComplementar(anyInt());
        return produto;
    }

    private static PedidoRepository.ProdutoPedidoPar itemPedido(Integer pedidoId, Integer produtoId) {
        PedidoRepository.ProdutoPedidoPar item = mock(PedidoRepository.ProdutoPedidoPar.class);
        when(item.getPedidoId()).thenReturn(pedidoId);
        when(item.getProdutoId()).thenReturn(produtoId);
        return item;
    }
}
