package com.buildnow.springbootapp.buildnowspringboot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class TempOCRListDTO {
    @JsonProperty("infoList")
    private List<TempOCRDTO> infoList;
}
