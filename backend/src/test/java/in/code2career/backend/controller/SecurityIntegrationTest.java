package in.code2career.backend.controller;

import in.code2career.backend.entity.User;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.enums.Difficulty;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.repository.TopicRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.AdminUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "management.health.mail.enabled=false",
        "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes-long",
        "app.api.auth-requests-per-minute=1000"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void leaderboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotCreateTopics() throws Exception {
        mockMvc.perform(post("/api/topics")
                        .with(csrf())
                        .with(user("regular-user").roles("USER"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Arrays",
                                  "description": "Array problems"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotAccessAdminUserManagement() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user("regular-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void administratorCanSearchAndFilterUserAccounts() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .username("admin-directory-test")
                .email("admin-directory-test@example.invalid")
                .password(passwordEncoder.encode("test-password-123"))
                .active(true)
                .build());

        mockMvc.perform(get("/api/admin/users")
                        .param("query", "admin-directory-test")
                        .with(user("admin-user").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("admin-directory-test@example.invalid"))
                .andExpect(jsonPath("$.content[0].active").value(true));

        mockMvc.perform(get("/api/admin/users")
                        .param("query", "admin-directory-test")
                        .param("active", "true")
                        .with(user("admin-user").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Transactional
    void inactiveAccountCannotLogIn() throws Exception {
        String email = "inactive-login-test@example.invalid";
        userRepository.saveAndFlush(User.builder()
                .username("inactive-login-test")
                .email(email)
                .password(passwordEncoder.encode("valid-password-123"))
                .active(false)
                .build());

        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"inactive-login-test@example.invalid","password":"valid-password-123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void deletingAccountRemovesUserOwnedActivity() {
        User user = userRepository.saveAndFlush(User.builder()
                .username("admin-delete-test")
                .email("admin-delete-test@example.invalid")
                .password(passwordEncoder.encode("test-password-123"))
                .build());
        jdbcTemplate.update("""
                INSERT INTO user_activity (user_id, activity_date, problems_solved, accepted_submissions, xp_earned)
                VALUES (?, ?, 1, 1, 25)
                """, user.getId(), LocalDate.now());

        adminUserService.deleteUser(user.getId());
        userRepository.flush();

        Assertions.assertFalse(userRepository.existsById(user.getId()));
        Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_activity WHERE user_id = ?", Integer.class, user.getId()));
    }

    @Test
    @Transactional
    void publicTestCaseEndpointDoesNotExposeHiddenCases() throws Exception {
        Topic topic = topicRepository.save(Topic.builder()
                .name("test-case-visibility-topic")
                .description("Test topic")
                .build());
        Problem problem = problemRepository.save(Problem.builder()
                .title("Test case visibility problem")
                .description("Test problem")
                .difficulty(Difficulty.EASY)
                .xpReward(10)
                .topic(topic)
                .build());
        testCaseRepository.save(TestCase.builder()
                .inputData("public input")
                .expectedOutput("public output")
                .isHidden(false)
                .problem(problem)
                .build());
        testCaseRepository.save(TestCase.builder()
                .inputData("secret input")
                .expectedOutput("secret output")
                .isHidden(true)
                .problem(problem)
                .build());

        mockMvc.perform(get("/api/testcases/problem/{problemId}", problem.getId())
                        .with(user("regular-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("public input")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void reversedActivityDateRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/me/activity")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-01")
                        .with(user("regular-user").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSubmissionIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/submissions/not-a-number")
                        .with(user("regular-user").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customRunRequiresLanguageAndCode() throws Exception {
        mockMvc.perform(post("/api/submissions/run")
                        .with(user("regular-user").roles("USER"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetRequestIsPublicAndDoesNotRevealUnknownEmails() throws Exception {
        mockMvc.perform(post("/api/users/password-reset/request")
                        .contentType("application/json")
                        .content("""
                                {"email":"unknown-user-for-password-reset-test@example.invalid"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void passwordResetSupportsLocalhostIpOrigin() throws Exception {
        mockMvc.perform(options("/api/users/password-reset/request")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"));
    }

    @Test
    @Transactional
    void passwordResetRequestDoesNotRevealEmailWhenDeliveryFails() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .username("password-reset-mail-failure-test")
                .email("password-reset-mail-failure@example.invalid")
                .password(passwordEncoder.encode("old-password-123"))
                .build());
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        mockMvc.perform(post("/api/users/password-reset/request")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-reset-mail-failure@example.invalid"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void passwordResetRequestValidatesEmail() throws Exception {
        mockMvc.perform(post("/api/users/password-reset/request")
                        .contentType("application/json")
                        .content("""
                                {"email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void passwordResetOtpUpdatesPasswordAndCanOnlyBeUsedOnce() throws Exception {
        String email = "password-reset-test@example.invalid";
        userRepository.saveAndFlush(User.builder()
                .username("password-reset-test")
                .email(email)
                .password(passwordEncoder.encode("old-password-123"))
                .build());

        String oldAccessToken = loginAndReadAccessToken(email, "old-password-123");

        mockMvc.perform(post("/api/users/password-reset/request")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-reset-test@example.invalid"}
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        Matcher otpMatcher = Pattern.compile("(?m)^Your password reset code is: (\\d{6})$")
                .matcher(messageCaptor.getValue().getText());
        Assertions.assertTrue(otpMatcher.find());
        String otp = otpMatcher.group(1);
        String incorrectOtp = otp.equals("000000") ? "000001" : "000000";

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-reset-test@example.invalid","otp":"%s","newPassword":"new-password-123"}
                                """.formatted(incorrectOtp)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-reset-test@example.invalid","otp":"%s","newPassword":"new-password-123"}
                                """.formatted(otp)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-reset-test@example.invalid","password":"new-password-123"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-reset-test@example.invalid","otp":"%s","newPassword":"another-password-123"}
                                """.formatted(otp)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void changingPasswordRevokesExistingAccessTokens() throws Exception {
        String email = "password-change-test@example.invalid";
        userRepository.saveAndFlush(User.builder()
                .username("password-change-test")
                .email(email)
                .password(passwordEncoder.encode("old-password-123"))
                .build());
        String accessToken = loginAndReadAccessToken(email, "old-password-123");

        mockMvc.perform(put("/api/users/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {"currentPassword":"old-password-123","newPassword":"new-password-123"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"password-change-test@example.invalid","password":"new-password-123"}
                                """))
                .andExpect(status().isOk());
    }

    private String loginAndReadAccessToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
