package org.games.futoshiki.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ InvalidSolutionException.class,
                        ConstraintViolationException.class,
                        IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(RuntimeException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler({GameNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(RuntimeException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler({UnsupportedOperationException.class})
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public String handleNotImplemented(RuntimeException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler({BoardLoadingException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleInternalServerError(RuntimeException ex) {
        return ex.getMessage();
    }

}
