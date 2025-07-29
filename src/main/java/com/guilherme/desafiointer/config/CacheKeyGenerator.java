package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.domain.Usuario;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@UtilityClass
public class CacheKeyGenerator {
    public static String historicoKey(Usuario usuario, LocalDateTime inicio,
                                      LocalDateTime fim, Pageable pageable) {
        Objects.requireNonNull(usuario, "Usuário não pode ser nulo");
        Objects.requireNonNull(inicio, "Data início não pode ser nula");
        Objects.requireNonNull(fim, "Data fim não pode ser nula");
        Objects.requireNonNull(pageable, "Pageable não pode ser nulo");
        return String.format("hist_%d_%s_%s_%d",
                usuario.getId(),
                inicio.toString(),
                fim.toString(),
                pageable.getPageNumber());  // Corrigido: usando getPageNumber() ao invés de pageNumber
    }

    public static String totaisKey(Usuario usuario, LocalDateTime inicio,
                                   LocalDateTime fim, String tipo) {
        return String.format("%d_%s_%s_%s",
                usuario.getId(),
                inicio.toString(),
                fim.toString(),
                tipo);
    }

    public static String cotacaoKey(String moeda) {
        return String.format("%s_%s",
                moeda,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
    }
}