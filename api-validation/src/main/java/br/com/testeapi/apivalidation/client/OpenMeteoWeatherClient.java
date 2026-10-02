package br.com.testeapi.apivalidation.client;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Param;
import feign.RequestLine;

public interface OpenMeteoWeatherClient {

    @RequestLine(
            "GET /v1/forecast?latitude={latitude}&longitude={longitude}"
                    + "&current={current}&timezone=auto"
    )
    JsonNode getCurrentWeather(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("current") String current
    );
}
