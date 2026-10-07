package com.desafiogc.RecommedationEngine.api.contexto;

import java.util.List;

import com.desafiogc.RecommedationEngine.model.contexto.FeriadoDTO;
import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import com.desafiogc.RecommedationEngine.model.contexto.LocalizacaoDTO;
import com.desafiogc.RecommedationEngine.service.ContextoService;
import com.desafiogc.RecommedationEngine.service.FeriadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contexto")
@RequiredArgsConstructor
public class ContextoController {

    private final ContextoService contextoService;
    private final FeriadoService feriadoService;

    @GetMapping("/clima")
    public ClimaDTO consultarClimaAtual(@RequestParam String cidade) {
        return contextoService.consultarClimaAtual(cidade);
    }

    @GetMapping("/localizacao")
    public LocalizacaoDTO consultarLocalizacao(@RequestParam String cidade) {
        return contextoService.consultarLocalizacao(cidade);
    }

    @GetMapping("/feriados")
    public List<FeriadoDTO> listarFeriados(
            @RequestParam int ano,
            @RequestParam(required = false) String uf
    ) {
        return feriadoService.listarFeriados(ano, uf);
    }
}
