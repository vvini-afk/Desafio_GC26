package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.api.ingestao.ClienteRequest;
import com.desafiogc.RecommedationEngine.api.ingestao.PedidoRequest;
import com.desafiogc.RecommedationEngine.api.ingestao.ProdutoRequest;
import com.desafiogc.RecommedationEngine.api.ingestao.PromocaoRequest;
import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.repository.CidadeRepository;
import com.desafiogc.RecommedationEngine.repository.PedidoRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import com.desafiogc.RecommedationEngine.repository.PromocaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IngestaoDadosService {
    private final ClienteRepository clienteRepository;
    private final CidadeRepository cidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final PromocaoRepository promocaoRepository;

    @Transactional
    public Cliente criarCliente(ClienteRequest request) {
        var cidade = request.cidadeId() != null
                ? cidadeRepository.findById(request.cidadeId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cidade não encontrada: " + request.cidadeId()))
                : request.cidade() == null || request.cidade().isBlank()
                        ? null
                        : cidadeRepository.findFirstByNomeIgnoreCaseOrderByIdAsc(request.cidade().trim())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Cidade não encontrada no cadastro do banco: " + request.cidade()));
        return clienteRepository.save(new Cliente(
                request.cnpj(), request.nome().trim(), request.perfil(), request.subsegmento(), request.cep(),
                request.hierarquiaId(), cidade, request.telefone(),
                request.ultimaCompra() == null ? null : request.ultimaCompra().atStartOfDay()
        ));
    }

    @Transactional
    public Produto criarProduto(ProdutoRequest request) {
        Produto produto = produtoRepository.save(new Produto(request.nome().trim(), request.categoria().trim(),
                request.precoBase(), request.precoMinimo()));
        for (Integer complementarId : request.produtosComplementaresIds()) {
            if (!produtoRepository.existsById(complementarId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Produto complementar não encontrado: " + complementarId);
            }
            produto.adicionarComplementar(complementarId);
        }
        return produto;
    }

    @Transactional
    public Pedido criarPedido(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        Pedido pedido = new Pedido(cliente, request.dataCompra().atStartOfDay());
        for (PedidoRequest.ItemRequest item : request.itens()) {
            Produto produto = produtoRepository.findById(item.produtoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto não encontrado: " + item.produtoId()));
            pedido.adicionarItem(produto, item.quantidade(), item.valorUnitario());
        }
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Promocao criarPromocao(PromocaoRequest request) {
        if (request.produtoId() == null && (request.categoria() == null || request.categoria().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o produto ou a categoria da promoção.");
        }
        if (request.produtoId() != null && request.categoria() != null && !request.categoria().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe somente o produto ou a categoria da promoção.");
        }
        if (request.produtoId() != null && !produtoRepository.existsById(request.produtoId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto da promoção não encontrado.");
        }
        try {
            return promocaoRepository.save(new Promocao(
                    request.nome().trim(), request.produtoId(),
                    request.categoria() == null ? null : request.categoria().trim(),
                    request.tipoDesconto(), request.valorDesconto(), request.inicio(), request.fim()
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
