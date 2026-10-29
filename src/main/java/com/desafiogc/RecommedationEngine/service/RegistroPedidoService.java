package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.api.ingestao.PedidoRequest;
import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.repository.PedidoRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RegistroPedidoService {
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final CrossSellService crossSellService;

    @Transactional
    public Pedido criar(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        Pedido pedido = new Pedido(cliente, request.dataCompra().atStartOfDay());
        for (PedidoRequest.ItemRequest item : request.itens()) {
            var produto = produtoRepository.findById(item.produtoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto não encontrado: " + item.produtoId()));
            pedido.adicionarItem(produto, item.quantidade(), item.valorUnitario());
        }
        Pedido pedidoSalvo = pedidoRepository.saveAndFlush(pedido);
        crossSellService.recalcular();
        return pedidoSalvo;
    }
}
