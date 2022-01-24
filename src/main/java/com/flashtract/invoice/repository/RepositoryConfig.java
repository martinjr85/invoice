package com.flashtract.invoice.repository;

import com.flashtract.invoice.model.Invoice;
import com.flashtract.invoice.model.validation.ContractUserValidator;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
@AllArgsConstructor
public class RepositoryConfig implements RepositoryRestConfigurer {

    private final Validator validator;
    private final ContractUserValidator contractUserValidator;

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.getExposureConfiguration()
                .forDomainType(Invoice.class)
                .withItemExposure((metadata, httpMethods) -> httpMethods.disable(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE))
                .withCollectionExposure((metadata, httpMethods) -> httpMethods.disable(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));

    }

    @Override
    public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingRepositoryEventListener) {
        validatingRepositoryEventListener.addValidator("beforeCreate", validator);
        validatingRepositoryEventListener.addValidator("beforeCreate", contractUserValidator);
    }
}
