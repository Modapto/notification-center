package gr.atc.modapto.exception;

import java.util.Arrays;

import gr.atc.modapto.controller.BaseAppResponse;
import static gr.atc.modapto.exception.CustomExceptions.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "Validation error";

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> invalidSecurityException(@NotNull AccessDeniedException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Invalid authorization parameters. You don't have the rights to access the resource or check the JWT and CSRF Tokens", ex.getCause()),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
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

    /*
     * Handles missing request body or missing data in request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseAppResponse<String>> handleHttpMessageNotReadableExceptionHandler(
            HttpMessageNotReadableException ex) {
        String errorMessage = "Required request body is missing or includes invalid data";

        // Check if instance is for InvalidFormat Validation
        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx
                && invalidFormatEx.getTargetType().isEnum()) {
            String fieldName = invalidFormatEx.getPath().getFirst().getFieldName();
            String invalidValue = invalidFormatEx.getValue().toString();

            // Format the error message according to the Validation Type failure
            errorMessage = String.format("Invalid value '%s' for field '%s'. Allowed values are: %s",
                    invalidValue, fieldName, Arrays.stream(invalidFormatEx.getTargetType().getEnumConstants())
                            .map(Object::toString).collect(Collectors.joining(", ")));

        }
        // Generic error handling
        return ResponseEntity.badRequest().body(BaseAppResponse.error(VALIDATION_ERROR, errorMessage));
    }

    @ExceptionHandler(ConstraintViolationException.class)
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

    /*
     * Handles validation for Method Parameters
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<BaseAppResponse<String>> validationExceptionHandler(
            @NonNull HandlerMethodValidationException ex) {
        return new ResponseEntity<>(BaseAppResponse.error(VALIDATION_ERROR, "Invalid input field"),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseAppResponse<String>> handleGeneralException(@NotNull Exception ex) {
        return new ResponseEntity<>(BaseAppResponse.error("An unexpected error occurred", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<BaseAppResponse<String>> handleDataNotFoundException(@NotNull DataNotFoundException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Requested resource not found in DB", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ModelMappingException.class)
    public ResponseEntity<BaseAppResponse<String>> handleModelMappingException(@NotNull ModelMappingException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Internal error in mapping process", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JwtTokenException.class)
    public ResponseEntity<BaseAppResponse<String>> handleJwtTokenException(@NotNull JwtTokenException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Authentication error", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedAssignmentUpdateException.class)
    public ResponseEntity<BaseAppResponse<String>> handleUnauthorizedAssignmentUpdateException(@NotNull UnauthorizedAssignmentUpdateException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Unauthorized assignment action", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }
}
