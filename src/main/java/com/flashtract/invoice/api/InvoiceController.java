package com.flashtract.invoice.api;

import com.flashtract.invoice.model.*;
import com.flashtract.invoice.model.exception.EntityNotFoundException;
import com.flashtract.invoice.model.exception.RequestValidationException;
import com.flashtract.invoice.repository.ContractRepository;
import com.flashtract.invoice.repository.InvoiceRepository;
import com.flashtract.invoice.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class InvoiceController {

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractService contractService;

    @PostMapping("/invoices")
    @Transactional
    public ResponseEntity<Invoice> create(@RequestBody @Validated Invoice invoice, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw RequestValidationException.builder().bindingResult(bindingResult).build();
        }

        Contract contract = contractService.findExistingContractById(invoice.getContractId());
        if(contract.getStatus() == Status.Completed) {
            String error = "Unable to add more invoices to this contract since it has been completed";
            throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "Contract completed",
                    null,
                    error.getBytes(),
                    StandardCharsets.UTF_8
            );
        }
        UUID contractId = contract.getContractId();
        Double existingContractTotal = invoiceRepository.sumValueByContractId(contractId);
        if (existingContractTotal == null) {
            log.warn("No prior existing invoices exist for contractId {}", contractId);
            contract.setStatus(Status.InProgress);
            existingContractTotal = 0.0;
        } else {
            log.info("Existing invoice total for contractId {} is {}", contractId, existingContractTotal);
        }

        double newInvoiceTotal = existingContractTotal + invoice.getValue();
        validateInvoiceAmount(existingContractTotal, newInvoiceTotal, invoice, contract);
        log.info("New invoice total is {}", newInvoiceTotal);

        if (newInvoiceTotal == contract.getAmount()) {
            log.info("Contract fulfilled...setting status to Completed");
            contract.setStatus(Status.Completed);
        }
        contractRepository.save(contract);

        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PutMapping("/invoices/{invoiceId}/void")
    @Transactional
    public ResponseEntity<Invoice> voidInvoice(@PathVariable UUID invoiceId) {
        Optional<Invoice> possibleInvoice = invoiceRepository.findById(invoiceId);
        if (possibleInvoice.isEmpty()) {
            throw new EntityNotFoundException("Invoice " + invoiceId);
        }

        Invoice invoice = possibleInvoice.get();

        Contract contract = contractService.findExistingContractById(invoice.getContractId());
        contract.setStatus(Status.InProgress);
        contractRepository.save(contract);

        invoice.setStatus(Status.Void);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    private void validateInvoiceAmount(double existingContractTotal, double newInvoiceTotal, Invoice invoice, Contract contract) {
        if (newInvoiceTotal > contract.getAmount()) {
            String allowableAmount = NumberFormat.getCurrencyInstance().format(contract.getAmount() - existingContractTotal);
            String error = "Invoice amount will exceed previously met contract amount.  "
                    + "The remaining allowable amount is " + allowableAmount;
            throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "Value too high",
                    null,
                    error.getBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
