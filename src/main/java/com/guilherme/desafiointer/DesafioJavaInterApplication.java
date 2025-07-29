package com.guilherme.desafiointer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching // Ativa o uso do Spring Cache
public class DesafioJavaInterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DesafioJavaInterApplication.class, args);
    }
}