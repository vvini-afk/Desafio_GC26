package com.desafiogc.RecommedationEngine.api.recomendacao;

import com.desafiogc.RecommedationEngine.service.MotorRecomendacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recomendacoes")
@RequiredArgsConstructor
public class RecomendacaoController {

    private final MotorRecomendacaoService motorRecomendacaoService;

    @GetMapping("/{clienteId}")
    public ResponseEntity<List<RecomendacaoDTO>> recomendar(@PathVariable Long clienteId) {
        return ResponseEntity.ok(motorRecomendacaoService.recomendar(clienteId));
    }
}
