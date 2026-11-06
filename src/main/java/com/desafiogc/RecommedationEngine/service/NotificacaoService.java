package com.desafiogc.RecommedationEngine.service;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.model.notificacao.Notificacao;
import com.desafiogc.RecommedationEngine.model.notificacao.TipoNotificacao;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.model.recomendacao.TipoDesconto;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;
import com.desafiogc.RecommedationEngine.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CANAL = "APP";
    private static final String STATUS_PENDENTE = "PENDENTE";

    private final ClienteRepository clienteRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final MotorRecomendacaoService motorRecomendacaoService;

    public ResultadoProcessamento notificarPromocao(Promocao promocao) {
        List<Cliente> elegiveis = clienteRepository.findClientesElegiveisParaPromocao(
                promocao.getProdutoId(), promocao.getCategoria());
        String mensagem = montarMensagemPromocao(promocao);
        int notificacoesCriadas = 0;
        int ignoradas = 0;

        for (Cliente cliente : elegiveis) {
            boolean jaExiste = notificacaoRepository.existsByCliente_IdAndTipoAndMensagem(
                    cliente.getId(), TipoNotificacao.PROMOCAO, mensagem);
            if (jaExiste) {
                ignoradas++;
                continue;
            }
            notificacaoRepository.save(new Notificacao(
                    cliente, TipoNotificacao.PROMOCAO, mensagem, CANAL, STATUS_PENDENTE));
            notificacoesCriadas++;
        }

        return new ResultadoProcessamento(elegiveis.size(), notificacoesCriadas, ignoradas);
    }

    public ResultadoProcessamento processarRecuperacao() {
        List<Cliente> clientesEmQuebra = clienteRepository.findClientesEmQuebraDeConsumo();
        LocalDate hoje = LocalDate.now(ZONA_BRASIL);
        LocalDateTime inicioDoDia = hoje.atStartOfDay();
        LocalDateTime inicioDoProximoDia = hoje.plusDays(1).atStartOfDay();
        int notificacoesCriadas = 0;
        int ignoradas = 0;

        for (Cliente cliente : clientesEmQuebra) {
            if (notificacaoRepository.existsByCliente_IdAndTipoAndCriadaEmBetween(
                    cliente.getId(), TipoNotificacao.RECUPERACAO, inicioDoDia, inicioDoProximoDia)) {
                ignoradas++;
                continue;
            }

            var recomendacoes = motorRecomendacaoService.recomendar(cliente.getId(), null, 3);
            if (recomendacoes.isEmpty()) {
                ignoradas++;
                continue;
            }

            String nomesProdutos = recomendacoes.stream()
                    .map(recomendacao -> recomendacao.produto().getNome())
                    .collect(Collectors.joining(", "));
            String mensagem = "Cliente em quebra de consumo. Sugestões de recuperação: " + nomesProdutos + ".";
            notificacaoRepository.save(new Notificacao(
                    cliente, TipoNotificacao.RECUPERACAO, mensagem, CANAL, STATUS_PENDENTE));
            notificacoesCriadas++;
        }

        return new ResultadoProcessamento(clientesEmQuebra.size(), notificacoesCriadas, ignoradas);
    }

    private static String montarMensagemPromocao(Promocao promocao) {
        String desconto = promocao.getTipoDesconto() == TipoDesconto.PERCENTUAL
                ? promocao.getValorDesconto().stripTrailingZeros().toPlainString() + "% de desconto"
                : "R$ " + promocao.getValorDesconto().setScale(2).toPlainString() + " de desconto";
        return "Promoção #" + promocao.getId() + " " + promocao.getNome() + ": "
                + desconto + ". Válida de "
                + promocao.getInicio().toLocalDate().format(FORMATO_DATA) + " até "
                + promocao.getFim().toLocalDate().format(FORMATO_DATA) + ".";
    }

    public record ResultadoProcessamento(int clientesAvaliados, int notificacoesCriadas, int ignoradas) { }
}
