package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "accom_operation_policy")
@Getter
@Setter
@ToString(exclude = "accom")
public class AccomOperationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accom_operation_policy_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false, unique = true)
    private Accom accom;

    @Column(nullable = false)
    private LocalDate operationStartDate;

    @Column(nullable = false)
    private LocalDate operationEndDate;

    @Column(nullable = false)
    private LocalTime checkInTime;

    @Column(nullable = false)
    private LocalTime checkOutTime;

    public void updatePolicy(LocalDate operationStartDate,
                             LocalDate operationEndDate,
                             LocalTime checkInTime,
                             LocalTime checkOutTime) {
        this.operationStartDate = operationStartDate;
        this.operationEndDate = operationEndDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    public void setAccom(Accom accom) {
        this.accom = accom;

        if (accom != null && accom.getOperationPolicy() != this) {
            accom.setOperationPolicy(this);
        }
    }
}