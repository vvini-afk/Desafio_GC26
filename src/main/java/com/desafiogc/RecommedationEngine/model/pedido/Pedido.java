package com.desafiogc.RecommedationEngine.model.pedido;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.model.produto.Produto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", referencedColumnName = "id_cliente")
    private Cliente cliente;

    @Column(name = "data_pedido", nullable = false)
    private LocalDateTime dataCompra;

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemPedido> itens = new ArrayList<>();

    public Pedido(Cliente cliente, LocalDateTime dataCompra) {
        if (cliente == null || dataCompra == null) {
            throw new IllegalArgumentException("Cliente e data de compra são obrigatórios.");
        }

        this.cliente = cliente;
        this.dataCompra = dataCompra;
        cliente.registrarPedido(this);
    }

    public void adicionarItem(Produto produto, int quantidade, BigDecimal valorUnitario) {
        if (produto == null) {
            throw new IllegalArgumentException("O produto é obrigatório.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }

        itens.add(new ItemPedido(this, produto, quantidade, valorUnitario));
    }
}
