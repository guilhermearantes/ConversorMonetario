package com.guilherme.desafiointer.controller;

import com.guilherme.desafiointer.dto.CotacaoResponseDTO;
import com.guilherme.desafiointer.dto.CotacaoStatusDTO;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/cotacao")
@RequiredArgsConstructor
public class CotacaoController {

    private final CotacaoServiceInterface cotacaoService;

    @GetMapping("/atual")
    public ResponseEntity<CotacaoResponseDTO> obterCotacaoAtual() {
        BigDecimal cotacao = cotacaoService.obterCotacao("USD");
        return ResponseEntity.ok(new CotacaoResponseDTO(
                cotacao,
                LocalDateTime.now(),
                "USD"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<CotacaoStatusDTO> verificarStatus() {
        try {
            BigDecimal cotacao = cotacaoService.obterCotacao("USD");
            return ResponseEntity.ok(new CotacaoStatusDTO(
                    true,
                    "API funcionando normalmente",
                    cotacao
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(new CotacaoStatusDTO(
                    false,
                    "Erro ao conectar com API: " + e.getMessage(),
                    null
            ));
        }
    }
}