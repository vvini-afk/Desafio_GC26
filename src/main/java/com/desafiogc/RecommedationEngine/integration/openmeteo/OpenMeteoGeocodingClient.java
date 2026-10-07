package com.desafiogc.RecommedationEngine.integration.openmeteo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpenMeteoGeocodingClient {

    private final RestClient restClient;

    public OpenMeteoGeocodingClient(
            RestClient.Builder restClientBuilder,
            @Value("${integrations.open-meteo.geocoding-url:https://geocoding-api.open-meteo.com/v1/search}")
            String geocodingUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(geocodingUrl).build();
    }

    public OpenMeteoGeocodingResponse.Location buscarCidade(String cidade) {
        try {
            OpenMeteoGeocodingResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("name", cidade)
                            .queryParam("count", 10)
                            .queryParam("language", "pt")
                            .queryParam("countryCode", "BR")
                            .build())
                    .retrieve()
                    .body(OpenMeteoGeocodingResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "A cidade não foi encontrada no Brasil pelo Open-Meteo.");
            }
            return response.results().getFirst();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar a localização no Open-Meteo.", exception);
        }
    }
}
