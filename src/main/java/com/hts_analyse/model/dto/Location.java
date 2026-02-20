package com.hts_analyse.model.dto;

import lombok.Data;

@Data
public class Location {

        private String displayName;
        private double latitude;
        private double longitude;
        private String city;    // ilçe
        private String state;   // il
}