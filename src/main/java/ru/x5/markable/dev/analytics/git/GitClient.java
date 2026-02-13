package ru.x5.markable.dev.analytics.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class GitClient {

    private static final String CACHE_DIR = "/var/git-cache/";

    public Path prepareRepository(String repoUrl) throws IOException, InterruptedException {
        String repoName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1)
                .replace(".git", "");

        Path repoPath = Paths.get(CACHE_DIR, repoName);

        if (Files.notExists(repoPath)) {
            execute(null, "git", "clone", repoUrl, repoPath.toString());
        } else {
            execute(repoPath, "git", "fetch", "--all");
        }

        return repoPath;
    }

    public List<String> collectStats(Path repoPath,
            LocalDate since,
            LocalDate until)
            throws IOException, InterruptedException {

        return execute(repoPath,
                "git",
                "log",
                "--all",
                "--since=" + since,
                "--until=" + until,
                "--pretty=format:%ae",
                "--numstat");
    }

    private List<String> execute(Path workingDir, String... command)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);

        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }

        pb.redirectErrorStream(true);

        Process process = pb.start();

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Git command failed: " + Arrays.toString(command));
        }

        return lines;
    }

}
