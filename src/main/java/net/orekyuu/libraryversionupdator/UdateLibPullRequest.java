package net.orekyuu.libraryversionupdator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ajoberstar.grgit.Grgit;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;

public class UdateLibPullRequest extends DefaultTask {

    /**
     * GitHub repository url.
     * example: https://github.com/orekyuu/Tuzigiri
     */
    private String githubPage;
    /**
     * GitHub AccessToken
     * see: https://github.com/settings/tokens
     */
    private String githubAccessToken;

    public String getGithubPage() {
        return githubPage;
    }

    public void setGithubPage(String githubPage) {
        this.githubPage = githubPage;
    }

    public String getGithubAccessToken() {
        return githubAccessToken;
    }

    public void setGithubAccessToken(String githubAccessToken) {
        this.githubAccessToken = githubAccessToken;
    }

    @TaskAction
    void updateLibrary() {
        LocalDate now = LocalDate.now();
        if (githubPage == null || githubAccessToken == null) {
            System.out.println("'githubPage' and 'githubAccessToken' are required fields.");
            return;
        }

        DependencyReport report = loadDependencyReport();
        if (report.getOutdated().getDependencies().isEmpty()) {
            System.out.println("Nothing to update.");
            return;
        }
        String branchName = commitAndPushToRemote(now);
        try {
            createPullRequest(report, branchName, now);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DependencyReport loadDependencyReport() {
        try {
            Path path = Paths.get(getProject().getRootDir().getAbsolutePath(), "build", "dependencyUpdates", "report.json");
            ObjectMapper mapper = new ObjectMapper();
            DependencyReport report = mapper.readValue(path.toFile(), DependencyReport.class);
            return report;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String commitAndPushToRemote(LocalDate now) {
        String branchName = "update-library-" + now.format(DateTimeFormatter.ofPattern("uuuu-MM-dd"));
        try(Grgit grgit = Grgit.open(openOp -> openOp.setDir(getProject().getRootDir()))) {
            grgit.checkout(op -> {
                op.setCreateBranch(true);
                op.setBranch(branchName);
            });
            grgit.add(op -> op.setPatterns(new HashSet<>(Collections.singletonList("build.gradle"))));
            grgit.commit(op -> op.setMessage("Update library " + now.format(DateTimeFormatter.ofPattern("uuuu/MM/dd"))));
            grgit.push();
        }
        return branchName;
    }

    private void createPullRequest(DependencyReport report, String branch, LocalDate now) throws IOException {
        URL url = URI.create(githubPage.replaceAll("github\\.com", "api.github.com/repos") + "/pulls").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "token " + githubAccessToken);
        connection.setRequestMethod("POST");
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            String json = "{ \"title\": \""+report.pullRequestTitle(now) +"\", " +
                    "\"body\": \""+report.pullRequestMessage()+"\"," +
                    "\"head\": \""+branch+"\"," +
                    "\"base\": \"master\"}";
            System.out.println(json);
            writer.write(json);
            writer.flush();
        }
        String responseMessage = connection.getResponseMessage();
        System.out.println(responseMessage);
        if (connection.getResponseCode() == 200) {
            System.out.println("Complete!");
        }
    }

}
