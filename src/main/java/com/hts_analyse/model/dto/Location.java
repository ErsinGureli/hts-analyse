package com.hts_analyse.model.dto;

import lombok.Data;

@Data
public class Location {

        private String displayName;
        private double lat;
        private double lon;
        private String city;    // ilçe
        private String state;   // il
}