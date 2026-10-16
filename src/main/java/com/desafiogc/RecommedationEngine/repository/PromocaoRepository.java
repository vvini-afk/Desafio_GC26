package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocaoRepository extends JpaRepository<Promocao, Integer> {
}
