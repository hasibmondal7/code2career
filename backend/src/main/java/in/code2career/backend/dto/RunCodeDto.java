package in.code2career.backend.dto;

import lombok.Data;

@Data
public class RunCodeDto {
    private String language;
    private String code;
    private String customInput;
}