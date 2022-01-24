package com.flashtract.invoice.repository;

import com.flashtract.invoice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.UUID;

@RepositoryRestResource
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    @Query("select sum(value) as contractTotal from Invoice where contractId=:contractId group by contractId")
    Double sumValueByContractId(UUID contractId);
}
