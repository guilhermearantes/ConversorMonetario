package com.guilherme.desafiointer.service.validator;

public class DocumentoValidationHolder {
    private static DocumentoValidationService instance;

    public static void setService(DocumentoValidationService service) {
        instance = service;
    }

    public static DocumentoValidationService getService() {
        if (instance == null) {
            instance = new DocumentoValidationService();
        }
        return instance;
    }
}
