package net.orekyuu.libraryversionupdator;

import groovy.json.JsonSlurper;
import org.ajoberstar.grgit.Grgit;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import se.patrikerdes.DependencyUpdate;

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
import java.util.List;

import static se.patrikerdes.Common.getOutDatedDependencies;

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

        List<DependencyUpdate> dependencyUpdates = loadDependencyReport();
        if (dependencyUpdates.isEmpty()) {
            System.out.println("Nothing to update.");
            return;
        }
        String branchName = commitAndPushToRemote(now);
        try {
            createPullRequest(dependencyUpdates, branchName, now);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<DependencyUpdate> loadDependencyReport() {
        Path path = Paths.get(getProject().getRootDir().getAbsolutePath(), "build", "dependencyUpdates", "report.json");
        Object dependencyUpdatesJson = new JsonSlurper().parse(path.toFile());
        return getOutDatedDependencies(dependencyUpdatesJson);
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

    private void createPullRequest(List<DependencyUpdate> report, String branch, LocalDate now) throws IOException {
        URL url = URI.create(githubPage.replaceAll("github\\.com", "api.github.com/repos") + "/pulls").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "token " + githubAccessToken);
        connection.setRequestMethod("POST");
        String title = "[" + now.format(DateTimeFormatter.ofPattern("uuuu-MM-dd"))+ "] Update library";
        StringBuilder message = new StringBuilder();
        message.append("# Updated libraries\\n");
        message.append("|Name|Version|\\n");
        message.append("|:--:|:--:|\\n");
        for (DependencyUpdate it : report) {
            message.append("|").append(it.groupAndName()).append("|");
            message.append(it.getOldVersion()).append(" -> ").append(it.getNewVersion()).append("|\\n  ");
        }

        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            String json = "{ \"title\": \""+title +"\", " +
                    "\"body\": \""+ message.toString() +"\"," +
                    "\"head\": \""+branch+"\"," +
                    "\"base\": \"master\"}";
            writer.write(json);
            writer.flush();
        }
        String responseMessage = connection.getResponseMessage();
        System.out.println(responseMessage);
    }

}
