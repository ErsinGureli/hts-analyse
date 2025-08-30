package com.hts_analyse.model.response;

import com.hts_analyse.model.dto.CommonContactDto;
import com.hts_analyse.model.dto.CommonContactShortDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonContactResponse {
    private int totalCommonCount;
    private List<CommonContactShortDto> commonCommunications;
    private List<CommonContactDto> commonContacts;
}