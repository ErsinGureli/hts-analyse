package com.hts_analyse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "base_station_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseStationInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_station_id", nullable = false, unique = true)
    private String baseStationId;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}