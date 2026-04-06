package com.Accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "carlist")
@Getter
@Setter
public class CarList {

    @Id
    @Column(name = "carno")
    private Long carNo;

    @Column(name = "carname")
    private String carName;

    @Column(name = "carcompany")
    private String carCompany;

    @Column(name = "carprice")
    private Integer carPrice;

    @Column(name = "carusepeople")
    private Integer carUsePeople;

    @Column(name = "carinfo")
    private String carInfo;

    @Column(name = "carimg")
    private String carImg;

    @Column(name = "carcategory")
    private String carCategory;
}
