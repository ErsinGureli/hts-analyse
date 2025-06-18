package com.hts_analyse.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityDistrict {
    private String city;     // 'İl'
    private String district; // 'İlçe'
}
