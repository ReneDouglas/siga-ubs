package br.com.tecsus.sigaubs.exceptions;

public class DomainConflictException extends RuntimeException {

    public DomainConflictException(String message) {
        super(message);
    }
}
