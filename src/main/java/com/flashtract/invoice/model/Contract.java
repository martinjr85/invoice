package com.flashtract.invoice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contract")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-char")
    private UUID contractId;

    @NotNull
    @Type(type = "uuid-char")
    private UUID userId;

    private String description;

    @NotNull
    @Min(1)
    private Double amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @OneToMany(mappedBy = "contractId")
    private List<Invoice> invoices;

    @PrePersist
    void prePersist() {
        status = Status.Approved;
        createdAt = Instant.now();
    }
}
