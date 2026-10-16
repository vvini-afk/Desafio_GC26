package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    List<Pedido> findByCliente_Id(Long clienteId);
}
