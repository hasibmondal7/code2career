package in.code2career.backend.service.impl;

import in.code2career.backend.dto.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeEvaluationServiceImplTest {

    private final CodeEvaluationServiceImpl service =
            new CodeEvaluationServiceImpl("http://localhost:8090", "");

    @Test
    void doesNotAcceptSubmissionsWithoutTestCases() {
        EvaluationResult result = service.evaluate("JAVA", "class Solution {}", List.of());

        assertEquals("SYSTEM_ERROR", result.getStatus());
        assertEquals(0L, result.getExecutionTimeMs());
    }
}
