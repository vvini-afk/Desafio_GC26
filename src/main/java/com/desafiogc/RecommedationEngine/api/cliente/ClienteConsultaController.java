package com.desafiogc.RecommedationEngine.api.cliente;

import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteConsultaController {
    private static final int LIMITE_CLIENTES = 50;

    private final ClienteRepository clienteRepository;

    @GetMapping("/vendedores")
    public ResponseEntity<List<RepresentanteDTO>> listarRepresentantes() {
        return ResponseEntity.ok(clienteRepository.listarRepresentantes().stream()
                .map(RepresentanteDTO::from)
                .toList());
    }

    @GetMapping
    public ResponseEntity<List<ClienteResumoDTO>> listarClientes(
            @RequestParam(required = false) Integer codRepresentante
    ) {
        if (codRepresentante != null) {
            List<ClienteResumoDTO> carteira = clienteRepository
                    .listarCarteiraPorRepresentante(codRepresentante)
                    .stream()
                    .map(ClienteResumoDTO::from)
                    .toList();
            return ResponseEntity.ok(carteira);
        }

        List<ClienteResumoDTO> clientes = clienteRepository
                .findAll(PageRequest.of(0, LIMITE_CLIENTES, Sort.by("id").ascending()))
                .map(ClienteResumoDTO::from)
                .getContent();
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/vendedor/{codRepresentante}")
    public ResponseEntity<VendedorPainelDTO> consultarPainelVendedor(
            @PathVariable Integer codRepresentante
    ) {
        return clienteRepository.buscarRepresentantePorCodigo(codRepresentante)
                .map(rep -> {
                    List<ClienteResumoDTO> clientes = clienteRepository
                            .listarCarteiraPorRepresentante(codRepresentante)
                            .stream()
                            .map(ClienteResumoDTO::from)
                            .toList();

                    long emQuebra = clientes.stream()
                            .filter(ClienteResumoDTO::emQuebraConsumo)
                            .count();

                    long totalCidades = clientes.stream()
                            .map(ClienteResumoDTO::cidade)
                            .filter(Objects::nonNull)
                            .distinct()
                            .count();

                    String ufs = clientes.stream()
                            .map(ClienteResumoDTO::uf)
                            .filter(uf -> uf != null && !uf.isBlank())
                            .distinct()
                            .sorted()
                            .collect(Collectors.joining(" • "));

                    return ResponseEntity.ok(new VendedorPainelDTO(
                            rep.getIdHierarquia(),
                            rep.getCodRepresentante(),
                            rep.getNomeVendedor(),
                            clientes.size(),
                            emQuebra,
                            totalCidades,
                            ufs.isBlank() ? "RS" : ufs,
                            clientes
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/quebra-consumo")
    public ResponseEntity<List<ClienteResumoDTO>> listarEmQuebraDeConsumo() {
        List<ClienteResumoDTO> clientes = clienteRepository.listarClientesEmQuebraComResumo()
                .stream()
                .map(ClienteResumoDTO::from)
                .toList();
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/busca")
    public ResponseEntity<ClienteResumoDTO> buscarPorCnpj(@RequestParam String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return clienteRepository.findByCnpjNormalizado(cnpj.trim())
                .map(ClienteResumoDTO::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
