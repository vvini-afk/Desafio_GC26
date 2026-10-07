package com.desafiogc.RecommedationEngine.model.contexto;

public record LocalizacaoDTO(
        String cidade,
        String estado,
        String pais,
        double latitude,
        double longitude,
        String fusoHorario
) {
}
