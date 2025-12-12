package com.example.chatvideo.exceptions;

/**
 * Formulaire d'enregistrement non valide
 */
public class InvalidRegistrationException extends Exception {

    public InvalidRegistrationException(final String message) {
        super(message);
    }

    public InvalidRegistrationException(final String message, Throwable cause) {
        super(message, cause);
    }

}
