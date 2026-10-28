package in.code2career.backend.config;

import in.code2career.backend.entity.User;
import in.code2career.backend.enums.UserRole;
import in.code2career.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Development-only reset and initial-admin bootstrap. */
@Component
@Profile({"dev", "dev-reset"})
@Order(1)
public class DevelopmentDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataInitializer.class);
    private static final String RESET_CONFIRMATION = "RESET_CODE2CAREER_DEVELOPMENT_DATA";
    private static final Pattern LOCAL_DISPOSABLE_DATABASE_URL =
            Pattern.compile("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1)(?::\\d+)?/code2career_dev(?:\\?.*)?");

    private final DataResetProperties resetProperties;
    private final BootstrapProperties bootstrapProperties;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String datasourceUrl;

    public DevelopmentDataInitializer(DataResetProperties resetProperties, BootstrapProperties bootstrapProperties,
                                      JdbcTemplate jdbcTemplate, UserRepository userRepository,
                                      PasswordEncoder passwordEncoder, Environment environment,
                                      @Value("${spring.datasource.url}") String datasourceUrl) {
        this.resetProperties = resetProperties;
        this.bootstrapProperties = bootstrapProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean resetFlagPresent = args.containsOption("app.data-reset.execute");
        List<String> resetValues = args.getOptionValues("app.data-reset.execute");
        boolean resetRequested = resetValues != null && resetValues.size() == 1
                && "true".equalsIgnoreCase(resetValues.get(0));
        if (resetProperties.isEnabled() || resetFlagPresent) {
            if (!environment.acceptsProfiles(Profiles.of("dev-reset"))) {
                throw new IllegalStateException("Data reset is only available with the dev-reset profile");
            }
            if (!resetProperties.isEnabled() || !resetRequested) {
                throw new IllegalStateException("Data reset requires APP_DATA_RESET_ENABLED=true and --app.data-reset.execute=true");
            }
            if (!RESET_CONFIRMATION.equals(resetProperties.getConfirmation())) {
                throw new IllegalStateException("Development reset requires the exact confirmation value");
            }
            verifyDisposableDatabase();
            resetUserGeneratedData();
        }
        createInitialAdminIfConfigured();
    }

    private void verifyDisposableDatabase() {
        if (!LOCAL_DISPOSABLE_DATABASE_URL.matcher(datasourceUrl).matches()) {
            throw new IllegalStateException("Reset is restricted to the local code2career_dev database");
        }
        String databaseName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        if (!"code2career_dev".equals(databaseName)) {
            throw new IllegalStateException("Connected database is not the expected code2career_dev database");
        }
    }

    private void resetUserGeneratedData() {
        // Keep Flyway history and catalog/seed content intact so a reset remains usable.
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
        jdbcTemplate.execute("DELETE FROM user_badges");
        jdbcTemplate.execute("DELETE FROM user_activity");
        jdbcTemplate.execute("DELETE FROM roadmap_progress");
        jdbcTemplate.execute("UPDATE pvp_rooms SET winner_id = NULL");
        jdbcTemplate.execute("DELETE FROM pvp_room_players");
        jdbcTemplate.execute("DELETE FROM pvp_rooms");
        jdbcTemplate.execute("DELETE FROM submissions");
        jdbcTemplate.execute("UPDATE topics SET admin_id = NULL");
        jdbcTemplate.execute("UPDATE problems SET admin_id = NULL");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("ALTER SEQUENCE users_id_seq RESTART WITH 1");
        log.warn("Development user-generated data reset completed");
    }

    private void createInitialAdminIfConfigured() {
        String email = normalize(bootstrapProperties.getAdminEmail());
        String username = trim(bootstrapProperties.getAdminUsername());
        String password = bootstrapProperties.getAdminPassword();
        if (email == null && username == null && (password == null || password.isBlank())) {
            return;
        }
        int passwordBytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (email == null || email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || username == null || username.length() < 3 || username.length() > 50
                || passwordBytes < 8 || passwordBytes > 72) {
            throw new IllegalStateException("Initial admin email, username, and an 8-72 byte password must all be configured");
        }
        User existingUser = userRepository.findByEmailIgnoreCase(email);
        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("Configured initial admin email belongs to a non-admin account");
            }
            return;
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Configured initial admin username is already in use");
        }
        userRepository.save(User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .active(true)
                .build());
        log.info("Initial administrator account created");
    }

    private String normalize(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
