package com.flashtract.invoice.service;

import com.flashtract.invoice.model.Contract;
import com.flashtract.invoice.model.EntityNotFoundException;
import com.flashtract.invoice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    public Contract findExistingContractById(UUID contractId) {
        Optional<Contract> possibleContract = contractRepository.findById(contractId);
        if (possibleContract.isEmpty()) {
            throw new EntityNotFoundException("Contract " + contractId);
        }

        return possibleContract.get();
    }
}
