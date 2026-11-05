package com.desafiogc.RecommedationEngine.api.catalogo;

import com.desafiogc.RecommedationEngine.model.produto.Produto;
import com.desafiogc.RecommedationEngine.model.recomendacao.Promocao;
import com.desafiogc.RecommedationEngine.repository.ProdutoRepository;
import com.desafiogc.RecommedationEngine.repository.PromocaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoController {
    private final ProdutoRepository produtoRepository;
    private final PromocaoRepository promocaoRepository;

    @GetMapping("/catalogo")
    public ResponseEntity<List<ProdutoCatalogoDTO>> listarCatalogoDisponivel() {
        List<Promocao> promocoesVigentes = promocaoRepository.findAll().stream()
                .filter(promocao -> promocao.vigente(LocalDate.now()))
                .toList();

        List<ProdutoCatalogoDTO> catalogo = produtoRepository.findAtivosComEstoqueDisponivel().stream()
                .map(produto -> ProdutoCatalogoDTO.from(produto, promocaoAplicavel(produto, promocoesVigentes)))
                .toList();
        return ResponseEntity.ok(catalogo);
    }

    private static Promocao promocaoAplicavel(Produto produto, List<Promocao> promocoes) {
        return promocoes.stream()
                .filter(promocao -> Objects.equals(promocao.getProdutoId(), produto.getId())
                        || (promocao.getCategoria() != null && produto.getCategoria() != null
                        && promocao.getCategoria().equalsIgnoreCase(produto.getCategoria())))
                .min(Comparator.comparing((Promocao promocao) ->
                                !Objects.equals(promocao.getProdutoId(), produto.getId()))
                        .thenComparing(Promocao::getId))
                .orElse(null);
    }
}
