package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    @Override
    @EntityGraph(attributePaths = "cidadeRef")
    Page<Cliente> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "cidadeRef")
    Optional<Cliente> findById(Integer id);

    @Query(value = """
            SELECT c.*
            FROM public.cliente c
            INNER JOIN public.quebra_de_consumo q
                ON q.id_cliente = c.id_cliente
            ORDER BY q.razao_atraso DESC
            """, nativeQuery = true)
    List<Cliente> findClientesEmQuebraDeConsumo();

    @Query(value = """
            SELECT c.*
            FROM public.cliente c
            WHERE regexp_replace(c.cnpj, '[^0-9]', '', 'g') = regexp_replace(:cnpj, '[^0-9]', '', 'g')
               OR c.cnpj = :cnpj
               OR c.cnpj = trim(:cnpj)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Cliente> findByCnpjNormalizado(@Param("cnpj") String cnpj);

    @Query(value = """
            SELECT h.id_hierarquia AS "idHierarquia",
                   h.cod_representante AS "codRepresentante",
                   h.nome_vendedor AS "nomeVendedor"
            FROM public.hierarquia h
            WHERE h.cod_representante = :codRepresentante
            LIMIT 1
            """, nativeQuery = true)
    Optional<RepresentanteProjecao> buscarRepresentantePorCodigo(
            @Param("codRepresentante") Integer codRepresentante);

    @Query(value = """
            SELECT c.id_cliente AS "id",
                   c.nome AS "nome",
                   c.segmento AS "perfil",
                   c.subsegmento AS "subsegmento",
                   ci.nome AS "cidade",
                   ci.uf AS "uf",
                   ci.regiao AS "regiao",
                   c.cnpj AS "cnpj",
                   CAST(c.ultima_compra AS date) AS "ultimaCompra",
                   (q.id_cliente IS NOT NULL) AS "emQuebraConsumo",
                   q.gravidade AS "gravidadeQuebra",
                   CAST(q.dias_sem_comprar AS double precision) AS "diasSemComprar",
                   CAST(q.intervalo_medio_dias AS double precision) AS "intervaloMedioDias",
                   CAST(q.total_pedidos AS bigint) AS "totalPedidos"
            FROM public.cliente c
            INNER JOIN public.hierarquia h ON h.id_hierarquia = c.id_hierarquia
            LEFT JOIN public.cidade ci ON ci.id_cidade = c.id_cidade
            LEFT JOIN public.quebra_de_consumo q ON q.id_cliente = c.id_cliente
            WHERE h.cod_representante = :codRepresentante
            ORDER BY (q.id_cliente IS NOT NULL) DESC,
                     COALESCE(q.razao_atraso, 0) DESC,
                     c.nome ASC
            """, nativeQuery = true)
    List<ClienteCarteiraProjecao> listarCarteiraPorRepresentante(
            @Param("codRepresentante") Integer codRepresentante);

    interface RepresentanteProjecao {
        Integer getIdHierarquia();
        Integer getCodRepresentante();
        String getNomeVendedor();
    }

    interface ClienteCarteiraProjecao {
        Integer getId();
        String getNome();
        String getPerfil();
        String getSubsegmento();
        String getCidade();
        String getUf();
        String getRegiao();
        String getCnpj();
        LocalDate getUltimaCompra();
        Boolean getEmQuebraConsumo();
        String getGravidadeQuebra();
        Double getDiasSemComprar();
        Double getIntervaloMedioDias();
        Long getTotalPedidos();
    }
}
