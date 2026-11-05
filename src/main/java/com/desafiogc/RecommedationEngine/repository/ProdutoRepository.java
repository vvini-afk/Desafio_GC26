package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.produto.Produto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {

    @Override
    @EntityGraph(attributePaths = "complementaresIds")
    List<Produto> findAll();

    @EntityGraph(attributePaths = "complementaresIds")
    @Query("""
            select p from Produto p
            where p.ativo = true
              and exists (
                  select 1 from EstoqueErp e
                  where e.produtoId = p.id
                    and e.quantidadeDisponivel > 0
              )
            order by p.id asc
            """)
    List<Produto> findAtivosComEstoqueDisponivel();
}
