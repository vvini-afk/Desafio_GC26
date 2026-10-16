package com.desafiogc.RecommedationEngine.model.cliente;

import com.desafiogc.RecommedationEngine.model.pedido.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    @Column(name = "id_cliente")
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "segmento")
    private String perfil;

    @Column(name = "subsegmento")
    private String subsegmento;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "cep")
    private String cep;

    @Column(name = "id_hierarquia")
    private Integer hierarquiaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cidade")
    private Cidade cidadeRef;

    @Column(name = "numero_celular")
    private String telefone;

    @Column(name = "ultima_compra")
    private LocalDateTime ultimaCompra;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> historico = new ArrayList<>();

    public Cliente(String cnpj, String nome, String perfil, String subsegmento, String cep,
                   Integer hierarquiaId, Cidade cidade, String telefone, LocalDateTime ultimaCompra) {
        this.cnpj = cnpj;
        this.nome = nome;
        this.perfil = perfil;
        this.subsegmento = subsegmento;
        this.cep = cep;
        this.hierarquiaId = hierarquiaId;
        this.cidadeRef = cidade;
        this.telefone = telefone;
        this.ultimaCompra = ultimaCompra;
    }

    public String getCidade() {
        return cidadeRef == null ? null : cidadeRef.getNome();
    }

    public void atualizarDados(String nome, String perfil, Cidade cidade, String telefone, LocalDateTime ultimaCompra) {
        this.nome = nome;
        this.perfil = perfil;
        this.cidadeRef = cidade;
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
