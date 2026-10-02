package br.com.testeapi.apivalidation;

import br.com.testeapi.apivalidation.client.BrasilApiHolidayClient;
import br.com.testeapi.apivalidation.client.OpenMeteoGeocodingClient;
import br.com.testeapi.apivalidation.client.OpenMeteoWeatherClient;
import br.com.testeapi.apivalidation.model.Holiday;
import com.fasterxml.jackson.databind.JsonNode;
import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        String city = args.length > 0 ? args[0] : "Porto Alegre";

        JacksonDecoder jsonDecoder = new JacksonDecoder();

        OpenMeteoGeocodingClient geocodingClient = Feign.builder()
                .decoder(jsonDecoder)
                .target(
                        OpenMeteoGeocodingClient.class,
                        "https://geocoding-api.open-meteo.com"
                );

        OpenMeteoWeatherClient weatherClient = Feign.builder()
                .decoder(jsonDecoder)
                .target(
                        OpenMeteoWeatherClient.class,
                        "https://api.open-meteo.com"
                );

        BrasilApiHolidayClient holidayClient = Feign.builder()
                .decoder(new JacksonDecoder())
                .target(
                        BrasilApiHolidayClient.class,
                        "https://brasilapi.com.br"
                );

        //API de localização e clima
        try {
            JsonNode geocoding = geocodingClient.searchCity(city, 1, "pt");
            JsonNode location = geocoding.path("results").path(0);

            if (location.isMissingNode()) {
                System.out.println("Nenhuma localização encontrada para " + city);
                return;
            }

            double latitude = location.path("latitude").asDouble();
            double longitude = location.path("longitude").asDouble();

            JsonNode weather = weatherClient.getCurrentWeather(
                    latitude,
                    longitude,
                    "temperature_2m,precipitation,weather_code,wind_speed_10m"
            );

            System.out.println("Local: " + location.path("name").asText());
            System.out.println("País: " + location.path("country").asText());
            System.out.println("Coordenadas: " + latitude + ", " + longitude);
            System.out.println("Condições atuais:");
            System.out.println(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(weather.path("current"))
            );
        } catch (feign.FeignException exception) {
            System.err.println("A API retornou HTTP" + exception.status());
            System.err.println(exception.contentUTF8());
        } catch (Exception exception) {
            System.err.println("Falha na consulta: " + exception.getMessage());
        }

        //API de calendário
        try {
            List<Holiday> holidays =
                    holidayClient.getByYearAndState(2026, "RS");

            System.out.println();
            System.out.println("Feriados de 2026 - RS");
            System.out.println("----------------------");

            for (Holiday holiday : holidays) {
                System.out.printf(
                        "%s | %s | %s%n",
                        holiday.date(),
                        holiday.name(),
                        holiday.type()
                );
            }
        } catch (FeignException exception) {
            System.err.println("A BrasilAPI retornou HTTP " + exception.status());
            System.err.println(exception.contentUTF8());
        }
    }
}