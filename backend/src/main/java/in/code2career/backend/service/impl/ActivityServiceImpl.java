package in.code2career.backend.service.impl;

import in.code2career.backend.dto.ActivityResponseDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.exception.InvalidCredentialsException;
import in.code2career.backend.repository.UserActivityRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.ActivityService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final UserActivityRepository activityRepository;
    private final UserRepository userRepository;

    public ActivityServiceImpl(UserActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getCurrentUserActivity(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Activity start date must not be after end date");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            throw new InvalidCredentialsException();
        }

        return activityRepository
                .findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(user.getId(), from, to)
                .stream()
                .map(activity -> new ActivityResponseDto(
                        activity.getActivityDate(),
                        activity.getProblemsSolved(),
                        activity.getAcceptedSubmissions(),
                        activity.getXpEarned()
                ))
                .toList();
    }
}
