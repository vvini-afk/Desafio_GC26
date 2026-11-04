package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.contexto.FeriadoDTO;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PontuadorContextualTest {

    private final PontuadorContextual service = new PontuadorContextual(
            27, 18, 0.5, 7, 8, 10, 6, 7,
            "0101 - REFRIGERANTE PET|0102 - REFRIGERANTE VIDRO|0103 - REFRIGERANTE LATA|0104 - AGUAS|0105 - CITRUS|0106 - AGUA 20 LITROS|0107 - AGUA 5 LITROS|0108 - AGUA SABORIZADA|0109 - SUCOS|0114 - ENERGETICO LATA|0115 - ENERGETICO PET",
            "0117 - VINHO VIDRO", "0101 - REFRIGERANTE PET|0109 - SUCOS|0117 - VINHO VIDRO",
            "0101 - REFRIGERANTE PET|0102 - REFRIGERANTE VIDRO|0103 - REFRIGERANTE LATA|0105 - CITRUS|0109 - SUCOS|0112 - CERVEJA VIDRO|0113 - CERVEJA LATA|0117 - VINHO VIDRO");

    @Test
    void pontuaCategoriaDeCalorIgnorandoAcentosEMaiusculas() {
        Produto produto = produto("0101 - REFRIGERANTE PET");
        ClimaDTO climaQuente = new ClimaDTO("Porto Alegre", 0, 0, LocalDateTime.now(), 30,
                30, 50, 0, 0, 2);

        PontuacaoContextual resultado = service.pontuar(produto,
                new ContextoRecomendacao(climaQuente, List.of(), List.of()));

        assertEquals(8, resultado.pontos());
        assertTrue(resultado.motivos().getFirst().contains("Clima quente"));
    }

    @Test
    void pontuaChuvaEProximoFeriado() {
        Produto produto = produto("0109 - SUCOS");
        ClimaDTO climaChuvoso = new ClimaDTO("Porto Alegre", 0, 0, LocalDateTime.now(), 22,
                22, 70, 0, 61, 5);
        FeriadoDTO feriado = new FeriadoDTO(LocalDate.now().plusDays(2), "Feriado", "nacional");

        PontuacaoContextual resultado = service.pontuar(produto,
                new ContextoRecomendacao(climaChuvoso, List.of(feriado), List.of("aviso")));

        assertEquals(13, resultado.pontos());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Condição chuvosa")));
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Feriado")));
        assertTrue(resultado.motivos().contains("aviso"));
    }

    @Test
    void pontuaVinhoEmFrioEChuva() {
        Produto produto = produto("0117 - VINHO VIDRO");
        ClimaDTO climaFrioEChuvoso = new ClimaDTO("Porto Alegre", 0, 0, LocalDateTime.now(), 12,
                12, 90, 1.2, 61, 5);

        PontuacaoContextual resultado = service.pontuar(produto,
                new ContextoRecomendacao(climaFrioEChuvoso, List.of(), List.of()));

        assertEquals(16, resultado.pontos());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Clima frio")));
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Condição chuvosa")));
    }

    @Test
    void rejeitaLimitesDeTemperaturaInconsistentes() {
        assertThrows(IllegalArgumentException.class, () -> new PontuadorContextual(
                18, 27, 0.5, 7, 8, 10, 6, 7,
                "", "", "", ""));
    }

    private static Produto produto(String categoria) {
        Produto produto = mock(Produto.class);
        when(produto.getCategoria()).thenReturn(categoria);
        return produto;
    }
}
