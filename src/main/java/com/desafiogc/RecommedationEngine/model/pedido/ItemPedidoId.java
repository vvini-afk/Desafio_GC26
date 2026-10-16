package com.desafiogc.RecommedationEngine.model.pedido;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ItemPedidoId implements Serializable {
    private Integer pedidoId;
    private Integer produtoId;
}
