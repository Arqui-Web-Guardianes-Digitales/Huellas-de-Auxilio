package com.upc.huellasdeauxilio.excepciones;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;

// Solo validación: los errores de permisos, inexistencia y conflicto conservan su estado.
@RestControllerAdvice
public class ValidacionControlador {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorValidacion> validarFormulario(MethodArgumentNotValidException error) {
        String mensaje = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getDefaultMessage()).distinct().sorted().collect(Collectors.joining(" "));
        return ResponseEntity.badRequest().body(new ErrorValidacion("VALIDATION_ERROR", mensaje));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorValidacion> validarServicio(ConstraintViolationException error) {
        String mensaje = error.getConstraintViolations().stream()
                .map(campo -> campo.getMessage()).distinct().sorted().collect(Collectors.joining(" "));
        return ResponseEntity.badRequest().body(new ErrorValidacion("VALIDATION_ERROR", mensaje));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorValidacion> responderEstado(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                .body(new ErrorValidacion("REQUEST_ERROR", error.getReason() == null ? "No se pudo completar la operación." : error.getReason()));
    }

    public record ErrorValidacion(String code, String message) {}
}
