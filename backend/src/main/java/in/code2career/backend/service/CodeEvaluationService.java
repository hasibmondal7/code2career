package in.code2career.backend.service;

import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.TestCase;
import java.util.List;

public interface CodeEvaluationService {
    EvaluationResult evaluate(String language, String code, List<TestCase> testCases);
    String executeCustomInput(String language, String code, String customInput);
}
