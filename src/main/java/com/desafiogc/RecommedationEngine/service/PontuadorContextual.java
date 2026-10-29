package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PontuadorContextual {
    private final double temperaturaQuente;
    private final double temperaturaFria;
    private final double precipitacaoChuvaMm;
    private final int janelaFeriadoDias;
    private final int pontosClima;
    private final int pontosFeriado;
    private final List<String> categoriasCalor;
    private final List<String> categoriasFrio;
    private final List<String> categoriasChuva;
    private final List<String> categoriasFeriado;

    public PontuadorContextual(
            @Value("${recommendation.context.temperature-hot-celsius:27}") double temperaturaQuente,
            @Value("${recommendation.context.temperature-cold-celsius:18}") double temperaturaFria,
            @Value("${recommendation.context.rain-minimum-mm:0.5}") double precipitacaoChuvaMm,
            @Value("${recommendation.context.holiday-window-days:7}") int janelaFeriadoDias,
            @Value("${recommendation.context.weather-points:8}") int pontosClima,
            @Value("${recommendation.context.holiday-points:7}") int pontosFeriado,
            @Value("${recommendation.context.hot-categories:REFRIGERANTE|AGUAS|AGUA SABORIZADA|AGUA 20 LITROS|AGUA 5 LITROS|CITRUS|SUCOS|ENERGETICO}") String categoriasCalor,
            @Value("${recommendation.context.cold-categories:}") String categoriasFrio,
            @Value("${recommendation.context.rain-categories:}") String categoriasChuva,
            @Value("${recommendation.context.holiday-categories:REFRIGERANTE|CITRUS|SUCOS|CERVEJA|VINHO}") String categoriasFeriado
    ) {
        if (temperaturaFria >= temperaturaQuente || precipitacaoChuvaMm < 0
                || janelaFeriadoDias < 0 || pontosClima < 0 || pontosFeriado < 0) {
            throw new IllegalArgumentException("Os limites e pesos de contexto são inválidos.");
        }
        this.temperaturaQuente = temperaturaQuente;
        this.temperaturaFria = temperaturaFria;
        this.precipitacaoChuvaMm = precipitacaoChuvaMm;
        this.janelaFeriadoDias = janelaFeriadoDias;
        this.pontosClima = pontosClima;
        this.pontosFeriado = pontosFeriado;
        this.categoriasCalor = separarCategorias(categoriasCalor);
        this.categoriasFrio = separarCategorias(categoriasFrio);
        this.categoriasChuva = separarCategorias(categoriasChuva);
        this.categoriasFeriado = separarCategorias(categoriasFeriado);
    }

    public PontuacaoContextual pontuar(Produto produto, ContextoRecomendacao contexto) {
        var motivos = new ArrayList<String>();
        double pontos = 0;
        String categoria = NormalizadorCategoria.normalizar(produto.getCategoria());
        ClimaDTO clima = contexto.clima();
        if (clima != null) {
            if (pontosClima > 0 && clima.temperaturaCelsius() >= temperaturaQuente
                    && corresponde(categoria, categoriasCalor)) {
                pontos += pontosClima;
                motivos.add(String.format(Locale.ROOT, "Clima quente (%.1f °C) favorece a categoria %s (+%d)",
                        clima.temperaturaCelsius(), produto.getCategoria(), pontosClima));
            } else if (pontosClima > 0 && clima.temperaturaCelsius() <= temperaturaFria
                    && corresponde(categoria, categoriasFrio)) {
                pontos += pontosClima;
                motivos.add(String.format(Locale.ROOT, "Clima frio (%.1f °C) favorece a categoria %s (+%d)",
                        clima.temperaturaCelsius(), produto.getCategoria(), pontosClima));
            }
            if (pontosClima > 0 && estaChovendo(clima) && corresponde(categoria, categoriasChuva)) {
                pontos += pontosClima;
                motivos.add(String.format(Locale.ROOT, "Condição chuvosa atual favorece a categoria %s (+%d)",
                        produto.getCategoria(), pontosClima));
            }
        }
        if (pontosFeriado > 0 && !contexto.feriadosProximos().isEmpty()
                && corresponde(categoria, categoriasFeriado)) {
            pontos += pontosFeriado;
            String nomes = contexto.feriadosProximos().stream().limit(2)
                    .map(feriado -> feriado.nome() + " (" + feriado.data() + ")")
                    .reduce((primeiro, segundo) -> primeiro + " e " + segundo)
                    .orElse("feriado próximo");
            motivos.add(nomes + " nos próximos " + janelaFeriadoDias
                    + " dias favorece a categoria " + produto.getCategoria() + " (+" + pontosFeriado + ")");
        }
        motivos.addAll(contexto.avisos());
        return new PontuacaoContextual(pontos, motivos);
    }

    private boolean estaChovendo(ClimaDTO clima) {
        int codigo = clima.codigoTempoWmo();
        boolean codigoChuvoso = (codigo >= 51 && codigo <= 67)
                || (codigo >= 80 && codigo <= 82)
                || (codigo >= 95 && codigo <= 99);
        return clima.precipitacaoMilimetros() >= precipitacaoChuvaMm || codigoChuvoso;
    }

    private static boolean corresponde(String categoria, List<String> categorias) {
        return categorias.stream().anyMatch(categoria::contains);
    }

    private static List<String> separarCategorias(String categorias) {
        if (categorias == null || categorias.isBlank()) {
            return List.of();
        }
        return Arrays.stream(categorias.split("\\|"))
                .map(NormalizadorCategoria::normalizar)
                .filter(valor -> !valor.isBlank())
                .toList();
    }
}
