package com.example.chatvideo.exceptions;

import com.example.chatvideo.payload.response.MessageResponse;
import jakarta.validation.ValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Nullable
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        MessageResponse apiResponse = new MessageResponse(ex.getMessage());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage).toList();

        if (errors.isEmpty()) {
            apiResponse.setMessage("format incorrect");
        } else {
            apiResponse.setMessage(errors.get(0));
        }

        logger.info(ex);
        return ResponseEntity
                .badRequest()
                .contentType(APPLICATION_JSON)
                .body(apiResponse);

    }

    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<MessageResponse> handlePayloadValidationException(ValidationException e) {
        MessageResponse apiResponse = new MessageResponse(e.getMessage());
        apiResponse.setMessage(e.getMessage());
        logger.info(e);
        return ResponseEntity
                .badRequest()
                .contentType(APPLICATION_JSON)
                .body(apiResponse);
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    protected ResponseEntity<MessageResponse> handleStorageException(InvalidRegistrationException e) {
        MessageResponse apiResponse = new MessageResponse(e.getMessage());
        apiResponse.setMessage(e.getMessage());
        logger.info(e);
        return ResponseEntity
                .badRequest()
                .contentType(APPLICATION_JSON)
                .body(apiResponse);
    }

    @ExceptionHandler(InvalidUserException.class)
    protected ResponseEntity<MessageResponse> handlePayloadValidationException(InvalidUserException e) {
        MessageResponse apiResponse = new MessageResponse(e.getMessage());
        apiResponse.setMessage(e.getMessage());
        logger.info(e);
        return ResponseEntity
                .badRequest()
                .contentType(APPLICATION_JSON)
                .body(apiResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<MessageResponse> handleStorageException(BadCredentialsException e) {
        MessageResponse apiResponse = new MessageResponse(e.getMessage());
        apiResponse.setMessage(e.getMessage());
        logger.warn(e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_JSON)
                .body(apiResponse);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<MessageResponse> handleStorageException(Exception e) {
        MessageResponse apiResponse = new MessageResponse(e.getMessage());
        apiResponse.setMessage("exception inattendue.");
        logger.error("exception inattendue.", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_JSON)
                .body(apiResponse);
    }

}
