package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.api.ingestao.ClienteRequest;
import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.repository.CidadeRepository;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CadastroClienteService {
    private final ClienteRepository clienteRepository;
    private final CidadeRepository cidadeRepository;

    @Transactional
    public Cliente criar(ClienteRequest request) {
        var cidade = request.cidadeId() != null
                ? cidadeRepository.findById(request.cidadeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cidade não encontrada: " + request.cidadeId()))
                : request.cidade() == null || request.cidade().isBlank()
                ? null
                : cidadeRepository.findFirstByNomeIgnoreCaseOrderByIdAsc(request.cidade().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cidade não encontrada no cadastro do banco: " + request.cidade()));

        return clienteRepository.save(new Cliente(
                request.cnpj(), request.nome().trim(), request.perfil(), request.subsegmento(), request.cep(),
                request.hierarquiaId(), cidade, request.telefone(),
                request.ultimaCompra() == null ? null : request.ultimaCompra().atStartOfDay()));
    }
}
