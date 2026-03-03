package com.hts_analyse.model.response;

import com.hts_analyse.model.dto.CommonContactMultiShortDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonContactMultiResponse {
    private int totalCommonCount;
    private List<CommonContactMultiShortDto> commonCommunications;
}
