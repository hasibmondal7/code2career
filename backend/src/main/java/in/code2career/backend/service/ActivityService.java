package in.code2career.backend.service;

import in.code2career.backend.dto.ActivityResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface ActivityService {

    List<ActivityResponseDto> getCurrentUserActivity(LocalDate from, LocalDate to);
}
