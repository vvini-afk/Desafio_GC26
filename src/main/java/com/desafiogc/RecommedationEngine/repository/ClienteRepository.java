package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
