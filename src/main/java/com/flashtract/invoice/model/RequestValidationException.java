package com.flashtract.invoice.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;

@Builder
@Getter
public class RequestValidationException extends RuntimeException {
    private BindingResult bindingResult;
}
