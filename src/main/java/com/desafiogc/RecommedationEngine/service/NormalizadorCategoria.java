package com.desafiogc.RecommedationEngine.service;

import java.text.Normalizer;
import java.util.Locale;

final class NormalizadorCategoria {

    private NormalizadorCategoria() {
    }

    static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
