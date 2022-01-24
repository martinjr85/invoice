package com.flashtract.invoice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice")
@Data
public class Invoice {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-char")
    private UUID invoiceId;

    @NotNull
    @Min(1)
    private Double value;

    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    @Type(type = "uuid-char")
    private UUID contractId;

    @Column(updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        status = Status.Approved;
    }
}
