package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    List<Pedido> findByCliente_Id(Integer clienteId);

    @Query(value = "SELECT pedido_id AS \"pedidoId\", produto_id AS \"produtoId\" " +
            "FROM item_pedido ORDER BY pedido_id, produto_id", nativeQuery = true)
    List<ProdutoPedidoPar> listarParesDeProdutos();

    interface ProdutoPedidoPar {
        Integer getPedidoId();
        Integer getProdutoId();
    }
}
