package com.desafiogc.RecommedationEngine.model.contexto;

import java.time.LocalDate;

public record FeriadoDTO(LocalDate data, String nome, String tipo) {
}
