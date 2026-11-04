package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.notificacao.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {
}
