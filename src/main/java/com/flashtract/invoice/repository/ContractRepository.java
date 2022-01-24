package com.flashtract.invoice.repository;

import com.flashtract.invoice.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.UUID;

@RepositoryRestResource
public interface ContractRepository extends JpaRepository<Contract, UUID> {
}
