package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.contexto.ClimaDTO;
import com.desafiogc.RecommedationEngine.model.contexto.ContextoRecomendacao;
import com.desafiogc.RecommedationEngine.model.contexto.FeriadoDTO;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ContextoRecomendacaoService {
    private static final Logger log = LoggerFactory.getLogger(ContextoRecomendacaoService.class);
    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ContextoService contextoService;
    private final FeriadoService feriadoService;
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

    public ContextoRecomendacaoService(
            ContextoService contextoService,
            FeriadoService feriadoService,
            @Value("${recommendation.context.temperature-hot-celsius:27}") double temperaturaQuente,
            @Value("${recommendation.context.temperature-cold-celsius:18}") double temperaturaFria,
            @Value("${recommendation.context.rain-minimum-mm:0.5}") double precipitacaoChuvaMm,
            @Value("${recommendation.context.holiday-window-days:7}") int janelaFeriadoDias,
            @Value("${recommendation.context.weather-points:8}") int pontosClima,
            @Value("${recommendation.context.holiday-points:7}") int pontosFeriado,
            @Value("${recommendation.context.hot-categories:Bebidas geladas|Sorvetes|Praia|Verão}") String categoriasCalor,
            @Value("${recommendation.context.cold-categories:Bebidas quentes|Cafés|Chás|Sopas|Agasalhos|Inverno}") String categoriasFrio,
            @Value("${recommendation.context.rain-categories:Guarda-chuva|Capa de chuva|Galochas}") String categoriasChuva,
            @Value("${recommendation.context.holiday-categories:Presentes|Festas|Decoração|Viagem|Alimentos}") String categoriasFeriado
    ) {
        if (temperaturaFria >= temperaturaQuente) {
            throw new IllegalArgumentException("O limite de temperatura fria deve ser menor que o limite de calor.");
        }
        if (precipitacaoChuvaMm < 0 || janelaFeriadoDias < 0 || pontosClima < 0 || pontosFeriado < 0) {
            throw new IllegalArgumentException("Limites e pesos de contexto não podem ser negativos.");
        }
        this.contextoService = contextoService;
        this.feriadoService = feriadoService;
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

    public ContextoRecomendacao consultar(String cidade) {
        LocalDate hoje = LocalDate.now(ZONA_BRASIL);
        List<String> avisos = new ArrayList<>();
        ClimaDTO clima = null;
        List<FeriadoDTO> feriadosProximos = new ArrayList<>();

        if (cidade == null || cidade.isBlank()) {
            avisos.add("Clima não considerado: cliente sem cidade cadastrada.");
        } else {
            try {
                clima = contextoService.consultarClimaAtual(cidade);
            } catch (ResponseStatusException exception) {
                log.warn("Não foi possível consultar o clima para a cidade '{}': {}", cidade, exception.getReason());
                avisos.add("Clima indisponível; score calculado sem o fator climático.");
            }
        }

        LocalDate limite = hoje.plusDays(janelaFeriadoDias);
        try {
            for (int ano = hoje.getYear(); ano <= limite.getYear(); ano++) {
                feriadoService.listarFeriados(ano, null).stream()
                        .filter(feriado -> !feriado.data().isBefore(hoje) && !feriado.data().isAfter(limite))
                        .forEach(feriadosProximos::add);
            }
        } catch (ResponseStatusException exception) {
            log.warn("Não foi possível consultar feriados próximos: {}", exception.getReason());
            avisos.add("Calendário de feriados indisponível; score calculado sem o fator de calendário.");
        }

        return new ContextoRecomendacao(clima, feriadosProximos, avisos);
    }

    public PontuacaoContextual pontuar(Produto produto, ContextoRecomendacao contexto) {
        List<String> motivos = new ArrayList<>();
        double pontos = 0;
        String categoria = normalizar(produto.getCategoria());
        ClimaDTO clima = contexto.clima();
        if (clima != null) {
            if (pontosClima > 0 && clima.temperaturaCelsius() >= temperaturaQuente
                    && corresponde(categoria, categoriasCalor)) {
                pontos += pontosClima;
                motivos.add(String.format(Locale.ROOT,
                        "Clima quente (%.1f °C) favorece a categoria %s (+%d)",
                        clima.temperaturaCelsius(), produto.getCategoria(), pontosClima));
            } else if (pontosClima > 0 && clima.temperaturaCelsius() <= temperaturaFria
                    && corresponde(categoria, categoriasFrio)) {
                pontos += pontosClima;
                motivos.add(String.format(Locale.ROOT,
                        "Clima frio (%.1f °C) favorece a categoria %s (+%d)",
                        clima.temperaturaCelsius(), produto.getCategoria(), pontosClima));
            }

            if (pontosClima > 0 && estaChovendo(clima) && corresponde(categoria, categoriasChuva)) {
                pontos += pontosClima;
                motivos.add(String.format(Locale.ROOT,
                        "Condição chuvosa atual favorece a categoria %s (+%d)",
                        produto.getCategoria(), pontosClima));
            }
        }

        if (pontosFeriado > 0 && !contexto.feriadosProximos().isEmpty()
                && corresponde(categoria, categoriasFeriado)) {
            pontos += pontosFeriado;
            String nomes = contexto.feriadosProximos().stream()
                    .limit(2)
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

    private boolean corresponde(String categoria, List<String> categorias) {
        return categorias.stream().anyMatch(categoria::contains);
    }

    private static List<String> separarCategorias(String categorias) {
        if (categorias == null || categorias.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(categorias.split("\\|"))
                .map(ContextoRecomendacaoService::normalizar)
                .filter(valor -> !valor.isBlank())
                .toList();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
