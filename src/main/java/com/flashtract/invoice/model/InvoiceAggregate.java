package com.flashtract.invoice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class InvoiceAggregate {
    private UUID contractId;
    private Double total;
}
