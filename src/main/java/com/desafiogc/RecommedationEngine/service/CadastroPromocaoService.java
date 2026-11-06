package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.api.ingestao.PromocaoRequest;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import com.desafiogc.RecommedationEngine.repository.PromocaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CadastroPromocaoService {
    private final ProdutoRepository produtoRepository;
    private final PromocaoRepository promocaoRepository;
    private final NotificacaoService notificacaoService;

    @Transactional
    public Promocao criar(PromocaoRequest request) {
        if (request.produtoId() == null && (request.categoria() == null || request.categoria().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o produto ou a categoria da promoção.");
        }
        if (request.produtoId() != null && request.categoria() != null && !request.categoria().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe somente o produto ou a categoria da promoção.");
        }
        if (request.produtoId() != null && !produtoRepository.existsById(request.produtoId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto da promoção não encontrado.");
        }
        try {
            Promocao promocao = promocaoRepository.save(new Promocao(request.nome().trim(), request.produtoId(),
                    request.categoria() == null ? null : request.categoria().trim(), request.tipoDesconto(),
                    request.valorDesconto(), request.inicio(), request.fim()));
            notificacaoService.notificarPromocao(promocao);
            return promocao;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
