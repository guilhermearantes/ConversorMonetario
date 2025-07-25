package com.guilherme.desafiointer.remessa.repository;

import com.guilherme.desafiointer.remessa.domain.Carteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarteiraRepository extends JpaRepository<Carteira, Long> {
}