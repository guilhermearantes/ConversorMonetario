package com.guilherme.desafiointer.service.processor;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface RemessaProcessor {
    Remessa processarRemessa(RemessaRequestDTO remessaRequestDTO);
    Page<Remessa> buscarHistorico(Usuario usuario, LocalDateTime inicio,
                                  LocalDateTime fim, Pageable pageable);
}