package org.games.futoshiki.exception;

public class BoardValidationException extends RuntimeException {

    public BoardValidationException(String message) {
        super(message);
    }
}
