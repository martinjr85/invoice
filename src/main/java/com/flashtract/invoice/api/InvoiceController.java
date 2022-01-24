package com.flashtract.invoice.api;

import com.flashtract.invoice.model.*;
import com.flashtract.invoice.repository.ContractRepository;
import com.flashtract.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import javax.swing.text.NumberFormatter;
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

    @PostMapping("/invoices")
    @Transactional
    public ResponseEntity<Invoice> create(@RequestBody @Validated Invoice invoice, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw RequestValidationException.builder().bindingResult(bindingResult).build();
        }

        Optional<Contract> possibleContract = contractRepository.findById(invoice.getContractId());
        if (possibleContract.isEmpty()) {
            throw new EntityNotFoundException("Contract " + invoice.getContractId());
        }

        Contract contract = possibleContract.get();
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
        log.info("New invoice total is {}", newInvoiceTotal);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        if (newInvoiceTotal == contract.getAmount()) {
            log.info("Contract fulfilled...setting status to Completed");
            contract.setStatus(Status.Completed);
        }
        contractRepository.save(contract);

        return ResponseEntity.ok(savedInvoice);
    }
}
