package com.desafiogc.RecommedationEngine.api.recomendacao;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.service.MotorRecomendacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/recomendacoes")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecomendacaoController {

    private final MotorRecomendacaoService motorRecomendacaoService;
    private final ClienteRepository clienteRepository;

    @GetMapping("/{clienteId}")
    public ResponseEntity<List<RecomendacaoDTO>> recomendar(
            @PathVariable Integer clienteId,
            @RequestParam(required = false) String cidade,
            @RequestParam(defaultValue = "10") int limite
    ) {
        validarLimite(limite);
        return ResponseEntity.ok(motorRecomendacaoService.recomendar(clienteId, cidade, limite).stream()
                .map(RecomendacaoDTO::from)
                .toList());
    }

    @GetMapping("/quebra-consumo")
    public ResponseEntity<LoteRecomendacoesDTO> recomendarClientesEmQuebra(
            @RequestParam(defaultValue = "10") int limiteClientes,
            @RequestParam(defaultValue = "10") int limitePorCliente
    ) {
        if (limiteClientes < 1 || limiteClientes > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O limite de clientes deve estar entre 1 e 25.");
        }
        validarLimite(limitePorCliente);

        var clientes = motorRecomendacaoService
                .recomendarClientesEmQuebra(limiteClientes, limitePorCliente).stream()
                .map(cliente -> new RecomendacoesClienteDTO(
                        cliente.clienteId(),
                        cliente.recomendacoes().stream().map(RecomendacaoDTO::from).toList()))
                .toList();
        return ResponseEntity.ok(new LoteRecomendacoesDTO(
                clientes.size(),
                clientes.stream().filter(cliente -> !cliente.recomendacoes().isEmpty()).count(),
                clientes
        ));
    }

    @GetMapping("/{clienteId}/diagnostico")
    public ResponseEntity<DiagnosticoRecomendacaoDTO> diagnosticar(
            @PathVariable Integer clienteId,
            @RequestParam(required = false) String cidade,
            @RequestParam(defaultValue = "10") int limite
    ) {
        validarLimite(limite);
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        var recomendacoes = motorRecomendacaoService.recomendar(clienteId, cidade, limite);
        var cidadeRegional = cliente.getCidadeRef();
        BigDecimal configurado = cidadeRegional == null ? null : cidadeRegional.getMultiplicador();
        BigDecimal aplicado = configurado == null || configurado.signum() <= 0
                ? BigDecimal.ONE
                : configurado;
        List<RecomendacaoDTO> itens = recomendacoes.stream().map(RecomendacaoDTO::from).toList();

        return ResponseEntity.ok(new DiagnosticoRecomendacaoDTO(
                clienteId,
                cidade == null || cidade.isBlank()
                        ? cliente.getCidade() != null ? cliente.getCidade()
                        : cliente.getCep() == null ? null : "CEP " + cliente.getCep()
                        : cidade.trim(),
                cidadeRegional == null ? null : cidadeRegional.getNome(),
                cidadeRegional == null ? null : cidadeRegional.getRegiao(),
                configurado,
                aplicado,
                itens.size(),
                recomendacoes.stream().filter(r -> r.jaComprado()).count(),
                recomendacoes.stream().filter(r -> !r.jaComprado()).count(),
                recomendacoes.stream().filter(r -> r.isCrossSell() && !r.jaComprado()).count(),
                itens
        ));
    }

    private static void validarLimite(int limite) {
        if (limite < 1 || limite > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O limite deve estar entre 1 e 100.");
        }
    }
}
