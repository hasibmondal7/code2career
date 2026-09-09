package in.code2career.backend.service.impl;

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
    public String evaluate(String code, List<TestCase> testCases) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tempDir.mkdirs();
        String absolutePath = tempDir.getAbsolutePath();

        try {
            // 1. Write Code to File
            File sourceFile = new File(tempDir, "Solution.java");
            FileWriter sourceWriter = new FileWriter(sourceFile);
            sourceWriter.write(code);
            sourceWriter.close();

            // 2. Compile Code (Single Compilation)
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
                return "COMPILATION_ERROR";
            }

            // 3. Execute Test Cases (Dynamic Looping)
            File inputFile = new File(tempDir, "input.txt");

            for (TestCase tc : testCases) {
                // Save specific input data for this test case
                String inputData = tc.getInputData() != null ? tc.getInputData() : "";
                FileWriter inputWriter = new FileWriter(inputFile);
                inputWriter.write(inputData);
                inputWriter.close();

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

                Process runProcess = new ProcessBuilder(runCmd).start();
                boolean isFinished = runProcess.waitFor(3, TimeUnit.SECONDS);

                if (!isFinished) {
                    runProcess.destroyForcibly();
                    return "TIME_LIMIT_EXCEEDED";
                }

                if (runProcess.exitValue() != 0) {
                    return "RUNTIME_ERROR";
                }

                // Verify output
                BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                String actualOutput = output.toString().trim();
                String expectedOutput = tc.getExpectedOutput().trim();

                if (!actualOutput.equals(expectedOutput)) {
                    return "WRONG_ANSWER"; // Break immediately on first failure
                }
            }

            return "ACCEPTED"; // All test cases passed

        } catch (Exception e) {
            e.printStackTrace();
            return "SYSTEM_ERROR";
        } finally {
            deleteDirectory(tempDir);
        }
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