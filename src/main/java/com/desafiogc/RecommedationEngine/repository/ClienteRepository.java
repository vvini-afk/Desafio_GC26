package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    @Override
    @EntityGraph(attributePaths = "cidadeRef")
    Page<Cliente> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "cidadeRef")
    Optional<Cliente> findById(Integer id);

    @Query(value = """
            SELECT c.*
            FROM public.cliente c
            INNER JOIN public.vw_clientes_quebra_consumo q
                ON q.id_cliente = c.id_cliente
            """, nativeQuery = true)
    List<Cliente> findClientesEmQuebraDeConsumo();
}
