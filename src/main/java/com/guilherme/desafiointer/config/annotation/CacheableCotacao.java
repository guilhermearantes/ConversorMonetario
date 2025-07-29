package com.guilherme.desafiointer.config.annotation;

import com.guilherme.desafiointer.config.constants.AppConstants;
import org.springframework.cache.annotation.Cacheable;

import java.lang.annotation.*;

/**
 * Anotação para cachear cotações de moedas.
 * As cotações são armazenadas por 1 hora e são invalidadas automaticamente.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Cacheable(cacheNames = AppConstants.CACHE_COTACOES)
@Documented
public @interface CacheableCotacao {
}