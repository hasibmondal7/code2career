package in.code2career.backend.service.impl;

import in.code2career.backend.dto.LoginDto;
import in.code2career.backend.dto.ChangePasswordDto;
import in.code2career.backend.dto.UserProfileUpdateDto;
import in.code2career.backend.dto.UserResponseDto;
import in.code2career.backend.dto.UserDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.exception.ConflictException;
import in.code2career.backend.exception.InvalidCredentialsException;
import in.code2career.backend.mapper.UserMapper;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.security.JwtUtil;
import in.code2career.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public User createUser(UserDto userDto) {
        String email = userDto.getEmail().trim().toLowerCase();
        String username = userDto.getUsername().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("An account with this username already exists");
        }

        User user = UserMapper.mapToEntity(userDto, passwordEncoder.encode(userDto.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String verifyLogin(LoginDto loginDto) {
        User user = userRepository.findByEmailIgnoreCase(loginDto.getEmail().trim());

        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return jwtUtil.generateToken(user.getEmail());
        }

        throw new InvalidCredentialsException();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null || !passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())) {
            throw new ConflictException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser() {
        return UserMapper.mapToResponseDto(getAuthenticatedUser());
    }

    @Override
    @Transactional
    public UserResponseDto updateCurrentUser(UserProfileUpdateDto profileUpdateDto) {
        User user = getAuthenticatedUser();
        String username = profileUpdateDto.getUsername().trim();
        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(username)) {
            throw new ConflictException("An account with this username already exists");
        }

        user.setUsername(username);
        user.setProfilePhoto(profileUpdateDto.getProfilePhoto());
        return UserMapper.mapToResponseDto(userRepository.save(user));
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
