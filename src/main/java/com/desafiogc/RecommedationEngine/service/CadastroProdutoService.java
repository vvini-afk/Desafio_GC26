package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.api.ingestao.ProdutoRequest;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CadastroProdutoService {
    private final ProdutoRepository produtoRepository;

    @Transactional
    public Produto criar(ProdutoRequest request) {
        Produto produto = produtoRepository.save(new Produto(request.nome().trim(), request.categoria().trim(),
                request.precoBase(), request.precoMinimo()));
        for (Integer complementarId : request.produtosComplementaresIds()) {
            if (!produtoRepository.existsById(complementarId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Produto complementar não encontrado: " + complementarId);
            }
            produto.adicionarComplementar(complementarId);
        }
        return produto;
    }
}
