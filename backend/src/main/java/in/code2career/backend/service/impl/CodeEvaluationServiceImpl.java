package in.code2career.backend.service.impl;

import in.code2career.backend.dto.CodeRunnerRequest;
import in.code2career.backend.dto.CodeRunnerResponse;
import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.service.CodeEvaluationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Service
public class CodeEvaluationServiceImpl implements CodeEvaluationService {

    private final RestClient runnerClient;

    public CodeEvaluationServiceImpl(
            @Value("${app.code-runner.url:http://localhost:8090}") String runnerUrl
    ) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                    new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(15));
            this.runnerClient = RestClient.builder()
                    .baseUrl(runnerUrl)
                    .requestFactory(requestFactory)
                    .build();
    }

    @Override
    public EvaluationResult evaluate(String code, List<TestCase> testCases) {
        CodeRunnerRequest request = new CodeRunnerRequest(
                code,
                null,
                testCases.stream()
                        .map(testCase -> new CodeRunnerRequest.RunnerTestCase(
                                testCase.getInputData(),
                                testCase.getExpectedOutput()
                        ))
                        .toList()
        );
        CodeRunnerResponse response;
        try {
            response = runnerClient.post()
                    .uri("/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CodeRunnerResponse.class);
        } catch (RestClientException exception) {
            return new EvaluationResult("SYSTEM_ERROR", 0L);
        }

        if (response == null) {
            return new EvaluationResult("SYSTEM_ERROR", 0L);
        }
        return new EvaluationResult(response.status(), response.executionTimeMs());
    }

    @Override
    public String executeCustomInput(String code, String customInput) {
        CodeRunnerRequest request = new CodeRunnerRequest(code, customInput, List.of());
        CodeRunnerResponse response;
        try {
            response = runnerClient.post()
                    .uri("/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CodeRunnerResponse.class);
        } catch (RestClientException exception) {
            return "System Error: code runner unavailable";
        }

        if (response == null) {
            return "System Error";
        }
        if ("ACCEPTED".equals(response.status())) {
            return response.output() == null ? "" : response.output();
        }
        return response.error() == null ? response.status() : response.error();
    }
}
