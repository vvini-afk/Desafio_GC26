package com.desafiogc.RecommedationEngine.api.cliente;

import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteConsultaController {
    private static final int LIMITE_CLIENTES = 50;

    private final ClienteRepository clienteRepository;

    @GetMapping
    public ResponseEntity<List<ClienteResumoDTO>> listarClientes() {
        List<ClienteResumoDTO> clientes = clienteRepository
                .findAll(PageRequest.of(0, LIMITE_CLIENTES, Sort.by("id").ascending()))
                .map(ClienteResumoDTO::from)
                .getContent();
        return ResponseEntity.ok(clientes);
    }
}
