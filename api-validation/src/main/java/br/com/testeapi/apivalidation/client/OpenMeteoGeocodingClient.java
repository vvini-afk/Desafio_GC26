package br.com.testeapi.apivalidation.client;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Param;
import feign.RequestLine;

public interface OpenMeteoGeocodingClient {

    @RequestLine(
            "GET /v1/search?name={name}&count={count}&language={language}&format=json"
    )
    JsonNode searchCity(
            @Param("name") String city,
            @Param("count") int count,
            @Param("language") String language
    );
}
