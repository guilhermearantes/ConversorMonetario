package com.guilherme.desafiointer.dto.integration.bcb;

import lombok.Data;
import java.util.List;

@Data
public class PTAXResponse {
    private List<PTAXValue> value;

    @Data
    public static class PTAXValue {
        private String cotacaoCompra;
        private String cotacaoVenda;
        private String dataHoraCotacao;
    }
}