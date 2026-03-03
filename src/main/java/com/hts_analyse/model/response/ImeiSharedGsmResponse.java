package com.hts_analyse.model.response;

import com.hts_analyse.model.dto.ImeiSharedGsmDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImeiSharedGsmResponse {
    private int totalImeiCount;
    private List<ImeiSharedGsmDto> sharedImeis;
}
