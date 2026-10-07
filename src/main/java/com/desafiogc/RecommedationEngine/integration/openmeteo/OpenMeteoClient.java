package com.desafiogc.RecommedationEngine.integration.openmeteo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpenMeteoClient {

    private final RestClient restClient;

    public OpenMeteoClient(
            RestClient.Builder restClientBuilder,
            @Value("${integrations.open-meteo.forecast-url:https://api.open-meteo.com/v1/forecast}") String forecastUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(forecastUrl).build();
    }

    public OpenMeteoResponse buscarClimaAtual(double latitude, double longitude) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar a API de previsão do Open-Meteo.",
                    exception
            );
        }
    }
}
