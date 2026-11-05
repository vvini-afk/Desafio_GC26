package com.desafiogc.RecommedationEngine.model.cliente;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;

@Entity
@Table(name = "cidade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cidade")
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @JdbcTypeCode(Types.CHAR)
    @Column(length = 2, nullable = false)
    private String uf;

    @Column(name = "regiao")
    private String regiao;

    @Column(name = "multiplicador", precision = 10, scale = 4)
    private BigDecimal multiplicador;
}
