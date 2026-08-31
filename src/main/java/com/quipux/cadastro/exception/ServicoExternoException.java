package com.quipux.cadastro.exception;

public class ServicoExternoException extends RuntimeException {
    public ServicoExternoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
