package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.api.recomendacao.RecomendacaoDTO;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotorRecomendacaoService {

    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public List<RecomendacaoDTO> recomendar(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado.");
        }

        return produtoRepository.findByAtivoTrueOrderByIdAsc().stream()
                .map(produto -> new RecomendacaoDTO(
                        produto.getId(),
                        produto.getNome(),
                        produto.getCategoria(),
                        1.0,
                        List.of("Produto ativo"),
                        null
                ))
                .toList();
    }
}
