package br.com.testeapi.apivalidation.client;

import br.com.testeapi.apivalidation.model.Holiday;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface BrasilApiHolidayClient {

    @RequestLine("GET /api/feriados/v1/{year}")
    @Headers("Accept: application/json")
    List<Holiday> getByYear(@Param("year") int year);

    @RequestLine("GET /api/feriados/v1/{year}?uf={state}")
    @Headers("Accept: application/json")
    List<Holiday> getByYearAndState(
            @Param("year") int year,
            @Param("state") String state
    );
}
