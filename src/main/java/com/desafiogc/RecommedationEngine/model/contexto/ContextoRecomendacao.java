package com.desafiogc.RecommedationEngine.model.contexto;

import java.util.List;

public record ContextoRecomendacao(
        ClimaDTO clima,
        List<FeriadoDTO> feriadosProximos,
        List<String> avisos
) {
    public ContextoRecomendacao {
        feriadosProximos = feriadosProximos == null ? List.of() : List.copyOf(feriadosProximos);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }
}
