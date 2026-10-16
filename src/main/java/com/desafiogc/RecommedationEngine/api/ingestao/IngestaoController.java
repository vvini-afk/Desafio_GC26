package com.desafiogc.RecommedationEngine.api.ingestao;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.service.IngestaoDadosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IngestaoController {
    private final IngestaoDadosService ingestaoDadosService;

    @PostMapping("/clientes")
    public ResponseEntity<RegistroDTO> criarCliente(@Valid @RequestBody ClienteRequest request) {
        Cliente cliente = ingestaoDadosService.criarCliente(request);
        return criado("clientes", cliente.getId(), "Cliente cadastrado.");
    }

    @PostMapping("/produtos")
    public ResponseEntity<RegistroDTO> criarProduto(@Valid @RequestBody ProdutoRequest request) {
        Produto produto = ingestaoDadosService.criarProduto(request);
        return criado("produtos", produto.getId(), "Produto cadastrado.");
    }

    @PostMapping("/pedidos")
    public ResponseEntity<RegistroDTO> criarPedido(@Valid @RequestBody PedidoRequest request) {
        Pedido pedido = ingestaoDadosService.criarPedido(request);
        return criado("pedidos", pedido.getId(), "Compra registrada.");
    }

    @PostMapping("/promocoes")
    public ResponseEntity<RegistroDTO> criarPromocao(@Valid @RequestBody PromocaoRequest request) {
        Promocao promocao = ingestaoDadosService.criarPromocao(request);
        return criado("promocoes", promocao.getId(), "Promoção cadastrada.");
    }

    private ResponseEntity<RegistroDTO> criado(String tipo, Integer id, String mensagem) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegistroDTO(id, tipo, mensagem));
    }
}
