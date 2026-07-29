package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.exceptions.DomainConflictException;
import br.com.tecsus.sigaubs.exceptions.ForbiddenOperationException;
import br.com.tecsus.sigaubs.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

@ControllerAdvice
public class SafeErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(SafeErrorHandler.class);

    @ExceptionHandler(ForbiddenOperationException.class)
    ResponseEntity<ProblemDetail> handleForbidden(ForbiddenOperationException exception) {
        return problem(HttpStatus.FORBIDDEN, "Operação não autorizada.", exception);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado.", exception);
    }

    @ExceptionHandler(DomainConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(DomainConflictException exception) {
        return problem(HttpStatus.CONFLICT, "A operação conflita com o estado atual.", exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.");
        detail.setType(URI.create("urn:sigaubs:error:validation"));
        detail.setTitle("Requisição inválida");
        detail.setProperty("fields", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField())
                .distinct()
                .toList());
        return ResponseEntity.badRequest().body(detail);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String publicMessage, RuntimeException exception) {
        log.warn("Operação recusada [{}]: {}", exception.getClass().getSimpleName(), publicMessage);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, publicMessage);
        detail.setType(URI.create("urn:sigaubs:error:" + status.value()));
        detail.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(detail);
    }
}
