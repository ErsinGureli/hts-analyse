package com.hts_analyse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "hts_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HtsRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "gsm_number")
    private String gsmNumber;

    @Column(name = "record_type")
    private String recordType;

    @Column(name = "other_number")
    private String otherNumber;

    @Column(name = "record_time")
    private LocalDateTime recordDatetime;

    @Column(name = "full_name")
    private String fullName;

    // Base station info
    @Column(name = "base_station_id")
    private String baseStationId;

    @Column(name = "operator")
    private String operator;

    @Column(name = "address")
    private String address;

    // Analysis results
    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
