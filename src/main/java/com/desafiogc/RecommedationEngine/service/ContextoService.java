package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoGeocodingClient;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoGeocodingResponse;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoClient;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoResponse;
import com.desafiogc.RecommedationEngine.model.contexto.LocalizacaoDTO;
import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContextoService {

    private final OpenMeteoGeocodingClient geocodingClient;
    private final OpenMeteoClient openMeteoClient;

    public ClimaDTO consultarClimaAtual(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cidade é obrigatória.");
        }

        OpenMeteoGeocodingResponse.Location location = geocodingClient.buscarCidade(cidade.trim());
        OpenMeteoResponse response = openMeteoClient.buscarClimaAtual(
                location.latitude(),
                location.longitude()
        );

        if (response == null || response.current() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "A API do Open-Meteo não retornou condições atuais para a localização."
            );
        }

        OpenMeteoResponse.Current current = response.current();
        return new ClimaDTO(
                location.name(),
                location.latitude(),
                location.longitude(),
                LocalDateTime.parse(current.time()),
                current.temperature_2m(),
                current.apparent_temperature(),
                current.relative_humidity_2m(),
                current.precipitation(),
                current.weather_code(),
                current.wind_speed_10m()
        );
    }

    public LocalizacaoDTO consultarLocalizacao(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cidade é obrigatória.");
        }
        OpenMeteoGeocodingResponse.Location location = geocodingClient.buscarCidade(cidade.trim());
        return new LocalizacaoDTO(
                location.name(), location.admin1(), location.country(),
                location.latitude(), location.longitude(), location.timezone()
        );
    }
}
