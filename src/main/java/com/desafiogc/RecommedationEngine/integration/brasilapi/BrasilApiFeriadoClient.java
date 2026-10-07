package com.desafiogc.RecommedationEngine.integration.brasilapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class BrasilApiFeriadoClient {

    private final RestClient restClient;

    public BrasilApiFeriadoClient(
            RestClient.Builder restClientBuilder,
            @Value("${integrations.brasil-api.base-url:https://brasilapi.com.br}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public List<FeriadoBrasilApiDTO> listarFeriados(int ano, String uf) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/feriados/v1/{ano}");
                        if (uf != null && !uf.isBlank()) {
                            builder.queryParam("uf", uf);
                        }
                        return builder.build(ano);
                    })
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<FeriadoBrasilApiDTO>>() {});
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar feriados na BrasilAPI.", exception);
        }
    }
}
