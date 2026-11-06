package com.desafiogc.RecommedationEngine.api.notificacao;

import com.desafiogc.RecommedationEngine.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {
    private final NotificacaoService notificacaoService;

    @PostMapping("/processar-recuperacao")
    public ResponseEntity<NotificacaoService.ResultadoProcessamento> processarRecuperacao() {
        return ResponseEntity.ok(notificacaoService.processarRecuperacao());
    }
}
