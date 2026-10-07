package com.desafiogc.RecommedationEngine.integration.openmeteo;

import java.util.List;

public record OpenMeteoGeocodingResponse(List<Location> results) {

    public record Location(
            String name,
            double latitude,
            double longitude,
            String country,
            String admin1,
            String timezone
    ) {
    }
}
