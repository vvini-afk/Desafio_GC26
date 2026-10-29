package com.desafiogc.RecommedationEngine.api.recomendacao;

import com.desafiogc.RecommedationEngine.service.CrossSellService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CrossSellController {

    private final CrossSellService crossSellService;

    @PostMapping("/processar-cross-sell")
    public ResponseEntity<CrossSellService.Resultado> recalcular() {
        return ResponseEntity.ok(crossSellService.recalcular());
    }
}
