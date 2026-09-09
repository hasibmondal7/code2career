package in.code2career.backend.service;

import in.code2career.backend.entity.TestCase;
import java.util.List;

public interface CodeEvaluationService {
    String evaluate(String code, List<TestCase> testCases);
}