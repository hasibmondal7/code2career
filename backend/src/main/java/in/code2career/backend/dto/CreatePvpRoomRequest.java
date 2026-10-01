package in.code2career.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record CreatePvpRoomRequest(
        @NotBlank @Size(max = 80) String roomName,
        @NotNull @Positive Long problemId,
        @NotNull @Min(5) @Max(60) Integer timeLimitMinutes,
        @NotNull @Min(2) @Max(2) Integer maxPlayers,
        @NotNull @Min(1) @Max(1000) Integer stakeCoins,
        @NotBlank @Pattern(regexp = "JAVA|CPP|PYTHON|JAVASCRIPT", flags = Pattern.Flag.CASE_INSENSITIVE) String language,
        boolean publicMatch
) {
}
