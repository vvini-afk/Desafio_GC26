package com.desafiogc.RecommedationEngine.repository;

import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            INNER JOIN public.vw_clientes_quebra_consumo q
                ON q.id_cliente = c.id_cliente
            """, nativeQuery = true)
    List<Cliente> findClientesEmQuebraDeConsumo();

    @Query(value = """
            SELECT c.id_cliente AS id,
                   c.nome AS nome,
                   c.segmento AS perfil,
                   c.subsegmento AS subsegmento,
                   cid.nome AS cidade,
                   cid.uf AS uf,
                   cid.regiao AS regiao,
                   c.cnpj AS cnpj,
                   c.cep AS cep,
                   COALESCE(c.ultima_compra::date, q.ultima_compra) AS ultimaCompra,
                   TRUE AS emQuebraConsumo,
                   CASE
                       WHEN q.dias_sem_comprar > q.intervalo_medio_dias * 3 THEN 'CRITICA'
                       ELSE 'ALTA'
                   END AS gravidadeQuebra,
                   q.dias_sem_comprar AS diasSemComprar,
                   q.intervalo_medio_dias AS intervaloMedioDias,
                   (SELECT COUNT(*) FROM public.pedido p WHERE p.id_cliente = c.id_cliente) AS totalPedidos
            FROM public.cliente c
            LEFT JOIN public.cidade cid ON cid.id_cidade = c.id_cidade
            INNER JOIN public.vw_clientes_quebra_consumo q ON q.id_cliente = c.id_cliente
            ORDER BY q.dias_sem_comprar DESC, c.nome
            """, nativeQuery = true)
    List<ClienteCarteiraProjecao> listarClientesEmQuebraComResumo();

    @Query(value = """
            SELECT c.id_cliente AS id,
                   c.nome AS nome,
                   c.segmento AS perfil,
                   c.subsegmento AS subsegmento,
                   cid.nome AS cidade,
                   cid.uf AS uf,
                   cid.regiao AS regiao,
                   c.cnpj AS cnpj,
                   c.cep AS cep,
                   c.ultima_compra::date AS ultimaCompra,
                   (q.id_cliente IS NOT NULL) AS emQuebraConsumo,
                   CASE
                       WHEN q.id_cliente IS NULL THEN NULL
                       WHEN q.dias_sem_comprar > q.intervalo_medio_dias * 3 THEN 'CRITICA'
                       ELSE 'ALTA'
                   END AS gravidadeQuebra,
                   q.dias_sem_comprar AS diasSemComprar,
                   q.intervalo_medio_dias AS intervaloMedioDias,
                   (SELECT COUNT(*) FROM public.pedido p WHERE p.id_cliente = c.id_cliente) AS totalPedidos
            FROM public.cliente c
            LEFT JOIN public.cidade cid ON cid.id_cidade = c.id_cidade
            LEFT JOIN public.vw_clientes_quebra_consumo q ON q.id_cliente = c.id_cliente
            INNER JOIN public.hierarquia h ON h.id_hierarquia = c.id_hierarquia
            WHERE h.cod_representante = :codRepresentante
            ORDER BY c.nome
            """, nativeQuery = true)
    List<ClienteCarteiraProjecao> listarCarteiraPorRepresentante(
            @Param("codRepresentante") Integer codRepresentante);

    @Query(value = """
            SELECT h.id_hierarquia AS idHierarquia,
                   h.cod_representante AS codRepresentante,
                   h.nome_vendedor AS nomeVendedor
            FROM public.hierarquia h
            WHERE h.cod_representante = :codRepresentante
            LIMIT 1
            """, nativeQuery = true)
    Optional<RepresentanteProjecao> buscarRepresentantePorCodigo(
            @Param("codRepresentante") Integer codRepresentante);

    @Query(value = """
            SELECT h.id_hierarquia AS idHierarquia,
                   h.cod_representante AS codRepresentante,
                   h.nome_vendedor AS nomeVendedor
            FROM public.hierarquia h
            WHERE h.cod_representante IS NOT NULL
            ORDER BY h.nome_vendedor
            """, nativeQuery = true)
    List<RepresentanteProjecao> listarRepresentantes();

    @Query(value = """
            SELECT c.*
            FROM public.cliente c
            WHERE regexp_replace(c.cnpj, '\\D', '', 'g') = regexp_replace(:cnpj, '\\D', '', 'g')
            LIMIT 1
            """, nativeQuery = true)
    Optional<Cliente> findByCnpjNormalizado(@Param("cnpj") String cnpj);

    interface ClienteCarteiraProjecao {
        Integer getId();
        String getNome();
        String getPerfil();
        String getSubsegmento();
        String getCidade();
        String getUf();
        String getRegiao();
        String getCnpj();
        String getCep();
        java.time.LocalDate getUltimaCompra();
        Boolean getEmQuebraConsumo();
        String getGravidadeQuebra();
        Double getDiasSemComprar();
        Double getIntervaloMedioDias();
        Long getTotalPedidos();
    }

    interface RepresentanteProjecao {
        Integer getIdHierarquia();
        Integer getCodRepresentante();
        String getNomeVendedor();
    }
}
