package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.produto.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByIdAsc();
}
