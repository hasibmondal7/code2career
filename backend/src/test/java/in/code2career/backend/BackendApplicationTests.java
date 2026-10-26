package in.code2career.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
