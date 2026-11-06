package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.notificacao.Notificacao;
import com.desafiogc.RecommedationEngine.model.notificacao.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {
    boolean existsByCliente_IdAndTipoAndMensagem(Integer clienteId, TipoNotificacao tipo, String mensagem);

    boolean existsByCliente_IdAndTipoAndCriadaEmBetween(
            Integer clienteId,
            TipoNotificacao tipo,
            LocalDateTime inicio,
            LocalDateTime fim);
}
