package com.flashtract.invoice.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InvoiceError {
    private String header;
    private String error;
}
