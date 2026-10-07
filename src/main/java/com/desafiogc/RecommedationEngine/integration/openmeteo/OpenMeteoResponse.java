package com.desafiogc.RecommedationEngine.integration.openmeteo;

public record OpenMeteoResponse(Current current) {

    public record Current(
            String time,
            double temperature_2m,
            double apparent_temperature,
            double relative_humidity_2m,
            double precipitation,
            int weather_code,
            double wind_speed_10m
    ) {
    }
}
