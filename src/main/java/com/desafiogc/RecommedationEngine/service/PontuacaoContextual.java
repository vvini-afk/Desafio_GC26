package com.desafiogc.RecommedationEngine.service;

import java.util.List;

public record PontuacaoContextual(double pontos, List<String> motivos) {
    public PontuacaoContextual {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
    }
}
