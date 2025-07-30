package com.guilherme.desafiointer.dto.integration.bcb;

import lombok.Data;
import java.util.List;

@Data
public class BCBResponse {
    private List<BCBCotacao> value;

    @Data
    public static class BCBCotacao {
        private String cotacaoCompra;
        private String cotacaoVenda;
        private String dataHoraCotacao;
    }
}