package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.cliente.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CidadeRepository extends JpaRepository<Cidade, Integer> {
    Optional<Cidade> findFirstByNomeIgnoreCaseOrderByIdAsc(String nome);
}
