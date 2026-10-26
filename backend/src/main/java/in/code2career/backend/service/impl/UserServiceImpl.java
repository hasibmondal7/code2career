package in.code2career.backend.service.impl;

import in.code2career.backend.dto.LoginDto;
import in.code2career.backend.dto.ChangePasswordDto;
import in.code2career.backend.dto.UserProfileUpdateDto;
import in.code2career.backend.dto.UserResponseDto;
import in.code2career.backend.dto.UserDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.entity.PasswordResetToken;
import in.code2career.backend.exception.ConflictException;
import in.code2career.backend.exception.InvalidCredentialsException;
import in.code2career.backend.mapper.UserMapper;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.repository.PasswordResetTokenRepository;
import in.code2career.backend.security.JwtUtil;
import in.code2career.backend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String mailFrom;
    private final long codeExpirationMinutes;
    private final String otpPepper;
    private static final int MAX_RESET_CODE_ATTEMPTS = 5;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            JavaMailSender mailSender,
            @Value("${app.password-reset.code-expiration-minutes:10}") long codeExpirationMinutes,
            @Value("${app.mail.from}") String mailFrom,
            @Value("${app.jwt.secret}") String otpPepper
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
        this.codeExpirationMinutes = codeExpirationMinutes;
        this.mailFrom = mailFrom;
        this.otpPepper = otpPepper;
    }

    @Override
    @Transactional
    public User createUser(UserDto userDto) {
        String email = userDto.getEmail().trim().toLowerCase(Locale.ROOT);
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
            return jwtUtil.generateToken(user.getEmail(), user.getTokenVersion());
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
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        passwordResetTokenRepository.deleteByEmailIgnoreCase(user.getEmail());
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim());
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByEmailIgnoreCase(user.getEmail());
        String otp = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email(user.getEmail())
                .tokenHash(hashToken(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(codeExpirationMinutes))
                .build());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Your Code2Career password reset code");
        message.setText("Your password reset code is: " + otp
                + "\n\nThis code expires in " + codeExpirationMinutes + " minutes and can be used once."
                + "\n\nIf you did not request this, you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Password reset email delivery failed for user ID {}", user.getId(), exception);
        }
    }

    @Override
    @Transactional
    public boolean resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email.trim();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByEmailIgnoreCaseForUpdate(normalizedEmail)
                .orElse(null);
        if (resetToken == null || !resetToken.getExpiresAt().isAfter(LocalDateTime.now())
                || resetToken.getAttemptCount() >= MAX_RESET_CODE_ATTEMPTS) {
            return false;
        }

        if (!MessageDigest.isEqual(
                resetToken.getTokenHash().getBytes(StandardCharsets.US_ASCII),
                hashToken(otp).getBytes(StandardCharsets.US_ASCII)
        )) {
            resetToken.setAttemptCount(resetToken.getAttemptCount() + 1);
            passwordResetTokenRepository.save(resetToken);
            return false;
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (user == null) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        passwordResetTokenRepository.deleteByEmailIgnoreCase(user.getEmail());
        return true;
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
        user.setBannerPhoto(profileUpdateDto.getBannerPhoto());
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

    private String hashToken(String token) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(otpPepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is not available", exception);
        }
    }
}
