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

class ContextoRecomendacaoServiceTest {

    private final ContextoService clima = mock(ContextoService.class);
    private final FeriadoService feriados = mock(FeriadoService.class);
    private final ContextoRecomendacaoService service = new ContextoRecomendacaoService(
            clima, feriados, 27, 18, 0.5, 7, 8, 7,
            "Bebidas geladas|Sorvetes|Praia|Verão", "Bebidas quentes|Cafés|Chás|Sopas|Agasalhos|Inverno",
            "Guarda-chuva|Capa de chuva|Galochas", "Presentes|Festas|Decoração|Viagem|Alimentos");

    @Test
    void pontuaCategoriaDeCalorIgnorandoAcentosEMaiusculas() {
        Produto produto = produto("VERAO");
        ClimaDTO climaQuente = new ClimaDTO("Porto Alegre", 0, 0, LocalDateTime.now(), 30,
                30, 50, 0, 0, 2);

        PontuacaoContextual resultado = service.pontuar(produto,
                new ContextoRecomendacao(climaQuente, List.of(), List.of()));

        assertEquals(8, resultado.pontos());
        assertTrue(resultado.motivos().getFirst().contains("Clima quente"));
    }

    @Test
    void pontuaChuvaEProximoFeriado() {
        Produto produto = produto("Presentes");
        ClimaDTO climaChuvoso = new ClimaDTO("Porto Alegre", 0, 0, LocalDateTime.now(), 22,
                22, 70, 0, 61, 5);
        FeriadoDTO feriado = new FeriadoDTO(LocalDate.now().plusDays(2), "Feriado", "nacional");

        PontuacaoContextual resultado = service.pontuar(produto,
                new ContextoRecomendacao(climaChuvoso, List.of(feriado), List.of("aviso")));

        assertEquals(7, resultado.pontos());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Feriado")));
        assertTrue(resultado.motivos().contains("aviso"));
    }

    @Test
    void rejeitaLimitesDeTemperaturaInconsistentes() {
        assertThrows(IllegalArgumentException.class, () -> new ContextoRecomendacaoService(
                clima, feriados, 18, 27, 0.5, 7, 8, 7,
                "", "", "", ""));
    }

    private static Produto produto(String categoria) {
        Produto produto = mock(Produto.class);
        when(produto.getCategoria()).thenReturn(categoria);
        return produto;
    }
}
