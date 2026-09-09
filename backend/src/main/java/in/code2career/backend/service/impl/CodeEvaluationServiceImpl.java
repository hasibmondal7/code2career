package in.code2career.backend.service.impl;

import in.code2career.backend.service.CodeEvaluationService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.TimeUnit;

@Service
public class CodeEvaluationServiceImpl implements CodeEvaluationService {

    private static final String TEMP_DIR = "temp_codes";

    @Override
    public String evaluateJavaCode(String code, String inputData) {
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) dir.mkdir();

        File sourceFile = new File(dir, "Solution.java");

        try {
            // 1. File Save
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(code);
            }

            // 2. Compile
            ProcessBuilder pbCompile = new ProcessBuilder("javac", sourceFile.getAbsolutePath());
            pbCompile.directory(dir);
            Process compileProcess = pbCompile.start();
            if (!compileProcess.waitFor(5, TimeUnit.SECONDS) || compileProcess.exitValue() != 0) {
                return "COMPILATION_ERROR";
            }

            // 3. Execute
            ProcessBuilder pbRun = new ProcessBuilder("java", "Solution");
            pbRun.directory(dir);
            Process runProcess = pbRun.start();

            // Inject Input
            if (inputData != null && !inputData.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    os.write((inputData + "\n").getBytes());
                    os.flush();
                }
            }

            // 4. Capture Output & TLE Check
            boolean finished = runProcess.waitFor(2, TimeUnit.SECONDS); // 2 Sec Time Limit Exceeded Check
            if (!finished) {
                runProcess.destroy();
                return "TIME_LIMIT_EXCEEDED";
            }

            if (runProcess.exitValue() != 0) {
                return "RUNTIME_ERROR";
            }

            return readStream(runProcess.getInputStream()).trim();

        } catch (Exception e) {
            return "SYSTEM_ERROR";
        } finally {
            // 5. Cleanup
            new File(dir, "Solution.class").delete();
            sourceFile.delete();
        }
    }

    private String readStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
