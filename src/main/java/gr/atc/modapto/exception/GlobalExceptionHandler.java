package gr.atc.modapto.exception;

import gr.atc.modapto.controller.BaseAppResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static gr.atc.modapto.exception.CustomExceptions.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> invalidSecurityException(@NotNull AccessDeniedException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Invalid authorization parameters. You don't have the rights to access the resource or check the JWT and CSRF Tokens", ex.getCause()),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> handleDtoValidationExceptions(@NotNull MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(BaseAppResponse.error("Validation failed", errors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> handleParameterValidationExceptions(@NotNull ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String paramName = violation.getPropertyPath().toString();
            // Remove the method name from the path
            paramName = paramName.substring(paramName.lastIndexOf('.') + 1);
            String message = violation.getMessage();
            errors.put(paramName, message);
        });

        return new ResponseEntity<>(BaseAppResponse.error("Validation failed", errors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<BaseAppResponse<String>> handleGeneralException(@NotNull Exception ex) {
        return new ResponseEntity<>(BaseAppResponse.error("An unexpected error occurred", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataNotFoundException.class)
    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    public ResponseEntity<BaseAppResponse<String>> handleDataNotFoundException(@NotNull DataNotFoundException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Requested resource not found in DB", ex.getMessage()), HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(ModelMappingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<BaseAppResponse<String>> handleModelMappingException(@NotNull ModelMappingException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Internal error in mapping process", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JwtTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<BaseAppResponse<String>> handleJwtTokenException(@NotNull JwtTokenException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Authentication error", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }


}
