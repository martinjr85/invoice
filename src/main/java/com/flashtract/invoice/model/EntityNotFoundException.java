package com.flashtract.invoice.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Getter
public class EntityNotFoundException extends HttpStatusCodeException {
    private String entityName;

    public EntityNotFoundException(String entityName) {
        super(HttpStatus.NOT_FOUND);
        this.entityName = entityName;
    }

}
