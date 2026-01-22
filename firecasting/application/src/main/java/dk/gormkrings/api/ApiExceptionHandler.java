package dk.gormkrings.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String message, List<String> details) {
    }

    @ExceptionHandler(ApiValidationException.class)
    public ResponseEntity<ApiError> handleApiValidation(ApiValidationException ex) {
        String msg = (ex.getMessage() == null || ex.getMessage().isBlank()) ? "Validation failed" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(msg, ex.getDetails()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage() == null ? "Invalid request" : ex.getMessage(), List.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        // Typically thrown when JSON cannot be parsed/bound. Surface a stable, actionable error.
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;
        List<String> details = new ArrayList<>();

        if (root instanceof MismatchedInputException mie) {
            String path = jsonPath(mie);
            String msg = mie.getOriginalMessage();
            details.add((path == null ? "body" : path) + ": " + (msg == null ? "invalid" : msg));
        } else if (root instanceof InvalidFormatException ife) {
            String path = jsonPath(ife);
            String msg = ife.getOriginalMessage();
            details.add((path == null ? "body" : path) + ": " + (msg == null ? "invalid format" : msg));
        } else if (root instanceof JsonProcessingException jpe) {
            String msg = jpe.getOriginalMessage();
            details.add("body: " + (msg == null ? "invalid JSON" : msg));
        } else {
            details.add("body: " + (root.getMessage() == null ? "invalid" : root.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Invalid JSON", details));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Validation failed", List.of(ex.getRequestPartName() + ": is required")));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> handleBadRequestBasics(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Invalid request", List.of(ex.getMessage() == null ? "invalid" : ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String field = fe.getField();
            String msg = fe.getDefaultMessage();
            details.add(field + ": " + (msg == null ? "invalid" : msg));
        }

        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            String obj = oe.getObjectName();
            String msg = oe.getDefaultMessage();
            details.add(obj + ": " + (msg == null ? "invalid" : msg));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Validation failed", details));
    }

    private static String jsonPath(JsonMappingException ex) {
        if (ex == null || ex.getPath() == null || ex.getPath().isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference r : ex.getPath()) {
            if (r.getFieldName() != null) {
                if (sb.length() > 0) sb.append('.');
                sb.append(r.getFieldName());
            } else if (r.getIndex() >= 0) {
                sb.append('[').append(r.getIndex()).append(']');
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
