package com.desafiogc.RecommedationEngine.model.cliente;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teste")
public class TesteClienteController {

    private final JdbcTemplate jdbcTemplate;

    public TesteClienteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/cliente")
    public ResponseEntity <Map<String, Object>> listarClientes() {

        int limite = 10;

        String sql = """
                SELECT *
                FROM public.cliente
                LIMIT ?
                """;

        List<Map<String, Object>> cliente =
                jdbcTemplate.queryForList(sql, limite);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("quantidadeRetornada", cliente.size());
        resposta.put("limite", limite);
        resposta.put("dados", cliente);

        return ResponseEntity.ok(resposta);
    }
}
