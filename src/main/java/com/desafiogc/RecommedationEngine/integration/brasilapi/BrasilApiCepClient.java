package com.desafiogc.RecommedationEngine.integration.brasilapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BrasilApiCepClient {

    private final RestClient restClient;

    public BrasilApiCepClient(
            RestClient.Builder restClientBuilder,
            @Value("${integrations.brasil-api.base-url:https://brasilapi.com.br}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public BrasilApiCepResponse buscar(String cep) {
        String cepLimpo = cep == null ? "" : cep.replaceAll("\\D", "");
        if (!cepLimpo.matches("\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O CEP do cliente precisa ter oito dígitos.");
        }

        try {
            BrasilApiCepResponse response = restClient.get()
                    .uri("/api/cep/v2/{cep}", cepLimpo)
                    .retrieve()
                    .body(BrasilApiCepResponse.class);
            if (response == null || response.city() == null || response.city().isBlank()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "A BrasilAPI não encontrou cidade para o CEP do cliente.");
            }
            return response;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao resolver o CEP do cliente na BrasilAPI.", exception);
        }
    }
}
