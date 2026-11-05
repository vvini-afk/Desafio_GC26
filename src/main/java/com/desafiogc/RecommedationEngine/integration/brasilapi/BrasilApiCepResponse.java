package com.desafiogc.RecommedationEngine.integration.brasilapi;

public record BrasilApiCepResponse(
        String cep,
        String state,
        String city,
        Location location
) {
    public record Location(String type, Coordinates coordinates) { }

    public record Coordinates(Object longitude, Object latitude) {
        public Double longitudeValue() {
            return toDouble(longitude);
        }

        public Double latitudeValue() {
            return toDouble(latitude);
        }

        private static Double toDouble(Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String text) {
                try {
                    return Double.valueOf(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    }
}
