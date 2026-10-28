package ru.itmo.pluginservice.simplepluginregistry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ResourceConflictException.class, DataIntegrityViolationException.class})
    ProblemDetail handleConflict(Exception exception) {
        String message = exception instanceof ResourceConflictException
                ? exception.getMessage()
                : "A resource with the same identifier already exists";
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
        detail.setTitle("Resource conflict");
        return detail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Resource not found");
        return detail;
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class
    })
    ProblemDetail handleValidation(Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request parameters failed validation"
        );
        detail.setTitle("Invalid request");
        return detail;
    }
}
