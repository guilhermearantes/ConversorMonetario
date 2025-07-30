package com.guilherme.desafiointer.controller;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.dto.remessa.RemessaResponseDTO;
import com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/remessas")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RemessaController {

    private final RemessaServiceInterface remessaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemessaResponseDTO realizarRemessa(@Valid @RequestBody RemessaRequestDTO request) {
        log.info("Processando remessa internacional: {}", request);
        Remessa remessa = remessaService.realizarRemessa(request);
        return RemessaResponseDTO.from(remessa);
    }
}