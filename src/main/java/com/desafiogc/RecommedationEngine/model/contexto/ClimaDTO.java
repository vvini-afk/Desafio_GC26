package com.desafiogc.RecommedationEngine.model.contexto;

import java.time.LocalDateTime;

public record ClimaDTO(
        String cidade,
        double latitude,
        double longitude,
        LocalDateTime observadoEm,
        double temperaturaCelsius,
        double sensacaoTermicaCelsius,
        double umidadeRelativaPercentual,
        double precipitacaoMilimetros,
        int codigoTempoWmo,
        double ventoQuilometrosPorHora
) {
}
