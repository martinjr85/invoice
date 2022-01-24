package com.flashtract.invoice.api;

import com.flashtract.invoice.model.EntityNotFoundException;
import com.flashtract.invoice.model.InvoiceError;
import com.flashtract.invoice.model.RequestValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class ExceptionHandlerControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ RequestValidationException.class })
    public ResponseEntity<InvoiceError> handleRequestValidationException(RequestValidationException exception) {
        String errors = Strings.join(
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + " " + error.getDefaultMessage()).collect(Collectors.toList())
                , ','
        );
        log.error("Request validation errors: {}", errors);
        return ResponseEntity.badRequest().body(
                InvoiceError.builder()
                        .header("Request validation errors")
                        .error(errors).build()
        );
    }

    @ExceptionHandler({ HttpClientErrorException.class })
    public ResponseEntity<InvoiceError> handleHttpClientErrorException(HttpClientErrorException exception) {
        log.error("{}, {}", exception.getStatusText(), exception.getResponseBodyAsString());
        return ResponseEntity.badRequest().body(
                InvoiceError.builder()
                        .header(exception.getStatusText())
                        .error(exception.getResponseBodyAsString()).build()
        );
    }

    @ExceptionHandler({ EntityNotFoundException.class })
    public ResponseEntity<InvoiceError> handleEntityNotFoundException(EntityNotFoundException exception) {
        log.error(exception.getMessage());
        return new ResponseEntity<>(InvoiceError.builder()
                .header("Not Found")
                .error(exception.getEntityName() + " not found").build(),
                HttpStatus.NOT_FOUND
        );
    }
}
