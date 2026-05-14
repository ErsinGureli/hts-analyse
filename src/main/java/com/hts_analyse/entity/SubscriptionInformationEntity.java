package com.hts_analyse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "subscription_informations")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionInformationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "gsm_number")
    private String gsmNumber;

    @Column(name = "status")
    private String status;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "address")
    private String address;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "district")
    private String district;

    @Column(name = "city")
    private String city;

    @Column(name = "identity_no", length = 100)
    private String identityNo;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "subscription_query_range")
    private String subscriptionQueryRange;

    @Column(name = "subscription_start_date")
    private String subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private String subscriptionEndDate;

    @Column(name = "operator")
    private String operator;
}
