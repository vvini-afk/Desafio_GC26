package com.desafiogc.RecommedationEngine.model.cliente;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "cliente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String perfil;

    private String cidade;

    private String telefone;

    @Column(name = "ultima_compra")
    private LocalDate ultimaCompra;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> historico = new ArrayList<>();

    public Cliente(String nome, String perfil, String cidade, String telefone, LocalDate ultimaCompra) {
        this.nome = nome;
        this.perfil = perfil;
        this.cidade = cidade;
        this.telefone = telefone;
        this.ultimaCompra = ultimaCompra;
    }

    public void atualizarDados(String nome, String perfil, String cidade, String telefone, LocalDate ultimaCompra) {
        this.nome = nome;
        this.perfil = perfil;
        this.cidade = cidade;
        this.telefone = telefone;
        this.ultimaCompra = ultimaCompra;
    }

    public List<Pedido> getHistorico() {
        return List.copyOf(historico);
    }

    public void registrarPedido(Pedido pedido) {
        Objects.requireNonNull(pedido, "pedido não pode ser nulo");
        if (!historico.contains(pedido)) {
            historico.add(pedido);
        }
        if (ultimaCompra == null || pedido.getDataCompra().isAfter(ultimaCompra)) {
            ultimaCompra = pedido.getDataCompra();
        }
    }
}
