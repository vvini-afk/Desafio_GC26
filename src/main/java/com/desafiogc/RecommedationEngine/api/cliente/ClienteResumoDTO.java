package com.desafiogc.RecommedationEngine.api.cliente;

import com.desafiogc.RecommedationEngine.model.cliente.Cidade;
import com.desafiogc.RecommedationEngine.model.cliente.Cliente;
import com.desafiogc.RecommedationEngine.repository.ClienteRepository;

import java.time.LocalDate;

public record ClienteResumoDTO(
        Integer id,
        String nome,
        String perfil,
        String subsegmento,
        String cidade,
        String uf,
        String regiao,
        String cnpj,
        LocalDate ultimaCompra,
        boolean emQuebraConsumo,
        String gravidadeQuebra,
        Double diasSemComprar,
        Double intervaloMedioDias,
        Long totalPedidos
) {
    public static ClienteResumoDTO from(Cliente cliente) {
        Cidade cidadeRef = cliente.getCidadeRef();
        return new ClienteResumoDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getPerfil(),
                cliente.getSubsegmento(),
                cliente.getCidade(),
                cidadeRef == null ? null : cidadeRef.getUf(),
                cidadeRef == null ? null : cidadeRef.getRegiao(),
                cliente.getCnpj(),
                cliente.getUltimaCompra() == null ? null : cliente.getUltimaCompra().toLocalDate(),
                false,
                null,
                null,
                null,
                null
        );
    }

    public static ClienteResumoDTO from(ClienteRepository.ClienteCarteiraProjecao projecao) {
        return new ClienteResumoDTO(
                projecao.getId(),
                projecao.getNome(),
                projecao.getPerfil(),
                projecao.getSubsegmento(),
                projecao.getCidade(),
                projecao.getUf() == null ? null : projecao.getUf().trim(),
                projecao.getRegiao(),
                projecao.getCnpj(),
                projecao.getUltimaCompra(),
                Boolean.TRUE.equals(projecao.getEmQuebraConsumo()),
                projecao.getGravidadeQuebra(),
                projecao.getDiasSemComprar(),
                projecao.getIntervaloMedioDias(),
                projecao.getTotalPedidos()
        );
    }
}
