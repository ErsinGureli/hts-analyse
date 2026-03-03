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
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "gsm_imei",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"gsm", "imei"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GsmImeiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "gsm", nullable = false, length = 20)
    private String gsm;

    @Column(name = "imei", nullable = false, length = 50)
    private String imei;
}
