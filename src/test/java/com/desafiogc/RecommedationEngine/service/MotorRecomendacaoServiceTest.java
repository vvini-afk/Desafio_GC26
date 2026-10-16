package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.repository.PedidoRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import com.desafiogc.RecommedationEngine.repository.PromocaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MotorRecomendacaoServiceTest {

    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final PromocaoRepository promocoes = mock(PromocaoRepository.class);
    private final ContextoRecomendacaoService contexto = mock(ContextoRecomendacaoService.class);
    private final MotorRecomendacaoService service = new MotorRecomendacaoService(
            clientes, produtos, pedidos, promocoes, contexto);
    private final ContextoRecomendacao contextoVazio = new ContextoRecomendacao(null, List.of(), List.of());

    @BeforeEach
    void configurarCenarioSemFatoresAdicionais() {
        reset(clientes, produtos, pedidos, promocoes, contexto);
        when(clientes.findById(1)).thenReturn(Optional.of(new Cliente(
                null, "Cliente", null, null, null, null, null, null, null)));
        when(contexto.consultar(null)).thenReturn(contextoVazio);
        when(contexto.pontuar(any(Produto.class), eq(contextoVazio)))
                .thenReturn(new PontuacaoContextual(0, List.of()));
        when(pedidos.findByCliente_Id(1)).thenReturn(List.of());
        when(promocoes.findAll()).thenReturn(List.of());
    }

    @Test
    void retornaProdutosPorScoreEDesempataPeloId() {
        Produto idMaior = produto(9, "Item B", "Categoria");
        Produto idMenor = produto(3, "Item A", "Categoria");
        when(produtos.findByAtivoTrueOrderByIdAsc()).thenReturn(List.of(idMaior, idMenor));

        var recomendacoes = service.recomendar(1, null);

        assertEquals(List.of(3, 9), recomendacoes.stream().map(r -> r.produto().getId()).toList());
        assertEquals(10, recomendacoes.getFirst().score());
        assertTrue(recomendacoes.getFirst().motivos().getFirst().contains("sem fatores"));
    }

    @Test
    void clienteInexistenteRetornaNotFoundSemConsultarOutrosDados() {
        when(clientes.findById(404)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.recomendar(404, null));

        assertEquals(404, erro.getStatusCode().value());
        verifyNoInteractions(produtos, pedidos, promocoes, contexto);
    }

    @Test
    void cidadeInformadaSubstituiCidadeDoCliente() {
        when(produtos.findByAtivoTrueOrderByIdAsc()).thenReturn(List.of());

        service.recomendar(1, "  Curitiba  ");

        verify(contexto).consultar("Curitiba");
    }

    private static Produto produto(Integer id, String nome, String categoria) {
        Produto produto = mock(Produto.class);
        when(produto.getId()).thenReturn(id);
        when(produto.getNome()).thenReturn(nome);
        when(produto.getCategoria()).thenReturn(categoria);
        when(produto.getComplementaresIds()).thenReturn(List.of());
        return produto;
    }
}
