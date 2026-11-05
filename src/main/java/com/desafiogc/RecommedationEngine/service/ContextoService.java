package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.integration.brasilapi.BrasilApiCepClient;
import com.desafiogc.RecommedationEngine.integration.brasilapi.BrasilApiCepResponse;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoClient;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoGeocodingClient;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoGeocodingResponse;
import com.desafiogc.RecommedationEngine.integration.openmeteo.OpenMeteoResponse;
import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import com.desafiogc.RecommedationEngine.model.contexto.LocalizacaoDTO;
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
    private final BrasilApiCepClient cepClient;

    public ClimaDTO consultarClimaAtual(String cidade) {
        return consultarClimaAtual(cidade, null, null);
    }

    public ClimaDTO consultarClimaAtual(String cidade, String uf, String cep) {
        BrasilApiCepResponse enderecoCep = null;
        if (cep != null && !cep.isBlank()) {
            try {
                enderecoCep = cepClient.buscar(cep);
                Double latitude = enderecoCep.location() == null || enderecoCep.location().coordinates() == null
                        ? null : enderecoCep.location().coordinates().latitudeValue();
                Double longitude = enderecoCep.location() == null || enderecoCep.location().coordinates() == null
                        ? null : enderecoCep.location().coordinates().longitudeValue();
                if (latitude != null && longitude != null) {
                    return buscarClima(latitude, longitude, enderecoCep.city());
                }
            } catch (ResponseStatusException exception) {
                if (cidade == null || cidade.isBlank()) {
                    throw exception;
                }
            }
        }

        String cidadeResolvida = cidade == null || cidade.isBlank()
                ? enderecoCep == null ? null : enderecoCep.city()
                : cidade.trim();
        String ufResolvida = uf == null || uf.isBlank()
                ? enderecoCep == null ? null : enderecoCep.state()
                : uf.trim();
        if (cidadeResolvida == null || cidadeResolvida.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O cliente precisa ter cidade vinculada ou um CEP válido para consultar o clima.");
        }

        OpenMeteoGeocodingResponse.Location location = geocodingClient.buscarCidade(cidadeResolvida, ufResolvida);
        return buscarClima(location.latitude(), location.longitude(), location.name());
    }

    private ClimaDTO buscarClima(double latitude, double longitude, String cidade) {
        OpenMeteoResponse response = openMeteoClient.buscarClimaAtual(latitude, longitude);
        if (response == null || response.current() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "A API do Open-Meteo não retornou condições atuais para a localização."
            );
        }

        OpenMeteoResponse.Current current = response.current();
        return new ClimaDTO(
                cidade,
                latitude,
                longitude,
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
