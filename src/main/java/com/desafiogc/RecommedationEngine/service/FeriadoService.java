package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.integration.brasilapi.BrasilApiFeriadoClient;
import com.desafiogc.RecommedationEngine.integration.brasilapi.FeriadoBrasilApiDTO;
import com.desafiogc.RecommedationEngine.model.contexto.FeriadoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeriadoService {

    private final BrasilApiFeriadoClient brasilApiFeriadoClient;

    public List<FeriadoDTO> listarFeriados(int ano, String uf) {
        if (ano < 1900 || ano > 2199) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O ano deve estar entre 1900 e 2199.");
        }
        String ufNormalizada = uf == null || uf.isBlank() ? null : uf.trim().toUpperCase();
        if (ufNormalizada != null && !ufNormalizada.matches("[A-Z]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A UF deve conter exatamente duas letras, por exemplo SP.");
        }

        return brasilApiFeriadoClient.listarFeriados(ano, ufNormalizada).stream()
                .map(this::toDto)
                .toList();
    }

    private FeriadoDTO toDto(FeriadoBrasilApiDTO feriado) {
        return new FeriadoDTO(feriado.date(), feriado.name(), feriado.type());
    }
}
