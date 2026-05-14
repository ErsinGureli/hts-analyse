package com.hts_analyse.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionInformationRecord {
    private String orderNo;
    private String gsmNumber;
    private String status;
    private String firstName;
    private String lastName;
    private String address;
    private String birthDate;
    private String birthPlace;
    private String district;
    private String city;
    private String identityNo;
    private String motherName;
    private String fatherName;
    private String subscriptionQueryRange;
    private String subscriptionStartDate;
    private String subscriptionEndDate;
    private String operator;
}
