package com.desafiogc.RecommedationEngine.model.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
