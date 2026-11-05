package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import com.desafiogc.RecommedationEngine.model.contexto.FeriadoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContextoRecomendacaoService {
    private static final Logger log = LoggerFactory.getLogger(ContextoRecomendacaoService.class);
    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ContextoService contextoService;
    private final FeriadoService feriadoService;
    private final int janelaFeriadoDias;

    public ContextoRecomendacaoService(
            ContextoService contextoService,
            FeriadoService feriadoService,
            @Value("${recommendation.context.holiday-window-days:7}") int janelaFeriadoDias
    ) {
        if (janelaFeriadoDias < 0) {
            throw new IllegalArgumentException("A janela de feriados não pode ser negativa.");
        }
        this.contextoService = contextoService;
        this.feriadoService = feriadoService;
        this.janelaFeriadoDias = janelaFeriadoDias;
    }

    public ContextoRecomendacao consultar(String cidade) {
        return consultar(cidade, null, null);
    }

    public ContextoRecomendacao consultar(String cidade, String uf, String cep) {
        LocalDate hoje = LocalDate.now(ZONA_BRASIL);
        List<String> avisos = new ArrayList<>();
        ClimaDTO clima = null;
        List<FeriadoDTO> feriadosProximos = new ArrayList<>();

        if ((cidade == null || cidade.isBlank()) && (cep == null || cep.isBlank())) {
            avisos.add("Clima não considerado: cliente sem cidade cadastrada e sem CEP.");
        } else {
            try {
                clima = contextoService.consultarClimaAtual(cidade, uf, cep);
            } catch (ResponseStatusException exception) {
                log.warn("Não foi possível consultar o clima para a cidade '{}': {}", cidade, exception.getReason());
                avisos.add("Clima indisponível; score calculado sem o fator climático.");
            }
        }

        LocalDate limite = hoje.plusDays(janelaFeriadoDias);
        try {
            for (int ano = hoje.getYear(); ano <= limite.getYear(); ano++) {
                feriadoService.listarFeriados(ano, null).stream()
                        .filter(feriado -> !feriado.data().isBefore(hoje) && !feriado.data().isAfter(limite))
                        .forEach(feriadosProximos::add);
            }
        } catch (ResponseStatusException exception) {
            log.warn("Não foi possível consultar feriados próximos: {}", exception.getReason());
            avisos.add("Calendário de feriados indisponível; score calculado sem o fator de calendário.");
        }

        return new ContextoRecomendacao(clima, feriadosProximos, avisos);
    }

}
