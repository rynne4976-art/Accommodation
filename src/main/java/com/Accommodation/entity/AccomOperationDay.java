package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(
        name = "accom_operation_day",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accom_operation_day",
                        columnNames = {"accom_id", "operation_date"}
                )
        }
)
@Getter
@Setter
@ToString(exclude = "accom")
public class AccomOperationDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accom_operation_day_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false)
    private Accom accom;

    @Column(name = "operation_date", nullable = false)
    private LocalDate operationDate;

    public void setAccom(Accom accom) {
        if (this.accom != null) {
            this.accom.getOperationDayList().remove(this);
        }

        this.accom = accom;

        if (accom != null && !accom.getOperationDayList().contains(this)) {
            accom.getOperationDayList().add(this);
        }
    }
}