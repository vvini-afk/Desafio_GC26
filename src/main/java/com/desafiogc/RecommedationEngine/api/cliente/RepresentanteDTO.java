package com.desafiogc.RecommedationEngine.api.cliente;

import com.desafiogc.RecommedationEngine.repository.ClienteRepository;

public record RepresentanteDTO(Integer idHierarquia, Integer codRepresentante, String nomeVendedor) {
    public static RepresentanteDTO from(ClienteRepository.RepresentanteProjecao representante) {
        return new RepresentanteDTO(
                representante.getIdHierarquia(),
                representante.getCodRepresentante(),
                representante.getNomeVendedor());
    }
}
