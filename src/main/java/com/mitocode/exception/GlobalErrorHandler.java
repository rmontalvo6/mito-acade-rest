package com.mitocode.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        // El detalle queda en el log; al cliente no se le expone el mensaje interno
        log.error("Error no controlado en {}", request.getDescription(false), ex);
        return construir("Ocurrió un error inesperado. Contacte al administrador.", request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<CustomErrorResponse> handleModelNotFoundException(ModelNotFoundException ex, WebRequest request) {
        return construir(ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<CustomErrorResponse> handleDuplicateException(DuplicateException ex, WebRequest request) {
        return construir(ex.getMessage(), request, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                           WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return construir(mensaje, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CustomErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                  WebRequest request) {
        String mensaje = "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue();
        return construir(mensaje, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<CustomErrorResponse> handlePropertyReference(PropertyReferenceException ex,
                                                                       WebRequest request) {
        // ?sort=campoQueNoExiste: es un error del cliente, no una incidencia del servidor.
        // Se usa getPropertyName() y no getMessage() para no filtrar la clase de dominio
        log.warn("Propiedad inexistente en {}: {}", request.getDescription(false), ex.getPropertyName());
        return construir("No existe la propiedad '" + ex.getPropertyName() + "' por la que se intenta ordenar.",
                request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CustomErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Cuerpo de petición ilegible en {}: {}", request.getDescription(false), ex.getMessage());
        return construir("El cuerpo de la petición no es un JSON válido.", request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Argumento inválido en {}: {}", request.getDescription(false), ex.getMessage());
        return construir("Petición inválida: " + ex.getMessage(), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CustomErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                        WebRequest request) {
        return construir("El método " + ex.getMethod() + " no está soportado en este recurso.", request,
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                            WebRequest request) {
        // Red de seguridad: restricciones de la BD (unicidad, claves foráneas) que el servicio no alcanzó a validar
        log.warn("Violación de integridad en {}: {}", request.getDescription(false), ex.getMostSpecificCause().getMessage());
        return construir("La operación viola una restricción de integridad de los datos.", request, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<CustomErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex,
                                                                        WebRequest request) {
        // Bloqueo optimista (@Version): otra petición modificó el mismo registro primero
        log.warn("Conflicto de concurrencia en {}: {}", request.getDescription(false), ex.getMessage());
        return construir("El registro fue modificado por otra operación. Vuelva a consultarlo e inténtelo de nuevo.",
                request, HttpStatus.CONFLICT);
    }

    private ResponseEntity<CustomErrorResponse> construir(String mensaje, WebRequest request, HttpStatus status) {
        CustomErrorResponse err = new CustomErrorResponse(LocalDateTime.now(), mensaje, request.getDescription(false));
        return new ResponseEntity<>(err, status);
    }

}