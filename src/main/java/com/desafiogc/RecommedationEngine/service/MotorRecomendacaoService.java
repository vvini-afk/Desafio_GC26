package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.cliente.Cidade;
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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorRecomendacaoService {
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final PromocaoRepository promocaoRepository;
    private final ContextoRecomendacaoService contextoRecomendacaoService;
    private final PontuadorRecomendacao pontuadorRecomendacao;

    public List<Recomendacao> recomendar(Integer clienteId, String cidadeOverride) {
        return recomendar(clienteId, cidadeOverride, 10);
    }

    public List<Recomendacao> recomendar(Integer clienteId, String cidadeOverride, int limite) {
        var cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        String cidade = cidadeOverride == null || cidadeOverride.isBlank()
                ? cliente.getCidade()
                : cidadeOverride.trim();
        Cidade cidadeDoCliente = cliente.getCidadeRef();
        BigDecimal multiplicadorRegional = multiplicadorValido(cidadeDoCliente);
        String regiao = cidadeDoCliente == null ? null : cidadeDoCliente.getRegiao();
        ContextoRecomendacao contexto = contextoRecomendacaoService.consultar(cidade);

        List<Pedido> historico = pedidoRepository.findByCliente_Id(clienteId);
        Map<String, Long> comprasPorCategoria = historico.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .collect(Collectors.groupingBy(
                        item -> NormalizadorCategoria.normalizar(item.getProduto().getCategoria()), Collectors.counting()));
        Set<Integer> produtosComprados = historico.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .map(item -> item.getProduto().getId())
                .collect(Collectors.toSet());
        List<Promocao> promocoesVigentes = promocaoRepository.findAll().stream()
                .filter(promocao -> promocao.vigente(LocalDate.now()))
                .toList();

        return produtoRepository.findAtivosComEstoqueDisponivel().stream()
                .map(produto -> pontuadorRecomendacao.pontuar(
                        produto, comprasPorCategoria, produtosComprados, promocoesVigentes, contexto))
                .filter(Recomendacao::recomendavel)
                .map(recomendacao -> recomendacao.aplicarMultiplicadorRegional(regiao, multiplicadorRegional))
                .sorted(Comparator.comparingDouble(Recomendacao::score).reversed()
                        .thenComparing(recomendacao -> recomendacao.produto().getId()))
                .limit(limite)
                .toList();
    }

    private static BigDecimal multiplicadorValido(Cidade cidade) {
        if (cidade == null || cidade.getMultiplicador() == null
                || cidade.getMultiplicador().signum() <= 0) {
            return BigDecimal.ONE;
        }
        return cidade.getMultiplicador();
    }
}
