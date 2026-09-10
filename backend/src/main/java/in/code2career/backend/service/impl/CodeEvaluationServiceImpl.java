package in.code2career.backend.service.impl;

import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.service.CodeEvaluationService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CodeEvaluationServiceImpl implements CodeEvaluationService {

    @Override
    public EvaluationResult evaluate(String code, List<TestCase> testCases) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tempDir.mkdirs();
        String absolutePath = tempDir.getAbsolutePath();

        long maxExecutionTime = 0;

        try {
            // 1. Write Code to File
            File sourceFile = new File(tempDir, "Solution.java");
            try (FileWriter sourceWriter = new FileWriter(sourceFile)) {
                sourceWriter.write(code);
            }

            // 2. Compile Code
            String[] compileCmd = {
                    "docker", "run", "--rm",
                    "-v", absolutePath + ":/app",
                    "-w", "/app",
                    "eclipse-temurin:17-alpine",
                    "javac", "Solution.java"
            };

            Process compileProcess = new ProcessBuilder(compileCmd).start();
            boolean isCompiled = compileProcess.waitFor(15, TimeUnit.SECONDS);

            if (!isCompiled || compileProcess.exitValue() != 0) {
                return new EvaluationResult("COMPILATION_ERROR", 0L);
            }

            // 3. Execute Test Cases
            File inputFile = new File(tempDir, "input.txt");

            for (TestCase tc : testCases) {
                // Save specific input data for this test case
                String inputData = tc.getInputData() != null ? tc.getInputData() : "";
                try (FileWriter inputWriter = new FileWriter(inputFile)) {
                    inputWriter.write(inputData);
                }

                // Run Docker container with isolated limits
                String[] runCmd = {
                        "docker", "run", "--rm",
                        "--memory=256m",
                        "--cpus=1.0",
                        "-v", absolutePath + ":/app",
                        "-w", "/app",
                        "eclipse-temurin:17-alpine",
                        "sh", "-c", "java Solution < input.txt"
                };

                long startTime = System.currentTimeMillis();

                Process runProcess = new ProcessBuilder(runCmd).start();
                boolean isFinished = runProcess.waitFor(3, TimeUnit.SECONDS);

                long endTime = System.currentTimeMillis();
                long timeTaken = endTime - startTime;
                maxExecutionTime = Math.max(maxExecutionTime, timeTaken);

                if (!isFinished) {
                    runProcess.destroyForcibly();
                    return new EvaluationResult("TIME_LIMIT_EXCEEDED", maxExecutionTime);
                }

                if (runProcess.exitValue() != 0) {
                    return new EvaluationResult("RUNTIME_ERROR", maxExecutionTime);
                }

                // Verify output
                String actualOutput = readProcessStream(runProcess.getInputStream());
                String expectedOutput = tc.getExpectedOutput().trim();

                if (!actualOutput.equals(expectedOutput)) {
                    return new EvaluationResult("WRONG_ANSWER", maxExecutionTime); // Break immediately on first failure
                }
            }

            return new EvaluationResult("ACCEPTED", maxExecutionTime); // All test cases passed

        } catch (Exception e) {
            e.printStackTrace();
            return new EvaluationResult("SYSTEM_ERROR", 0L);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Override
    public String executeCustomInput(String code, String customInput) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tempDir.mkdirs();
        String absolutePath = tempDir.getAbsolutePath();

        try {
            // 1. Code Write
            File sourceFile = new File(tempDir, "Solution.java");
            try (FileWriter sourceWriter = new FileWriter(sourceFile)) {
                sourceWriter.write(code);
            }

            // 2. Compile
            String[] compileCmd = {
                    "docker", "run", "--rm",
                    "-v", absolutePath + ":/app",
                    "-w", "/app",
                    "eclipse-temurin:17-alpine",
                    "javac", "Solution.java"
            };

            Process compileProcess = new ProcessBuilder(compileCmd).start();
            if (!compileProcess.waitFor(15, TimeUnit.SECONDS) || compileProcess.exitValue() != 0) {
                return readProcessStream(compileProcess.getErrorStream()); // Compilation error return korbe
            }

            // 3. Execution with Custom Input
            File inputFile = new File(tempDir, "input.txt");
            try (FileWriter inputWriter = new FileWriter(inputFile)) {
                inputWriter.write(customInput != null ? customInput : "");
            }

            String[] runCmd = {
                    "docker", "run", "--rm",
                    "--memory=256m",
                    "--cpus=1.0",
                    "-v", absolutePath + ":/app",
                    "-w", "/app",
                    "eclipse-temurin:17-alpine",
                    "sh", "-c", "java Solution < input.txt"
            };

            Process runProcess = new ProcessBuilder(runCmd).start();
            boolean isFinished = runProcess.waitFor(5, TimeUnit.SECONDS);

            if (!isFinished) {
                runProcess.destroyForcibly();
                return "Time Limit Exceeded";
            }

            if (runProcess.exitValue() != 0) {
                return readProcessStream(runProcess.getErrorStream()); // Runtime error return korbe
            }

            // Success hole console output return korbe
            return readProcessStream(runProcess.getInputStream());

        } catch (Exception e) {
            return "System Error: " + e.getMessage();
        } finally {
            deleteDirectory(tempDir);
        }
    }

    // Helper method to read streams cleanly
    private String readProcessStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString().trim();
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}