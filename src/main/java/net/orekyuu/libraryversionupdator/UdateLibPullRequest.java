package net.orekyuu.libraryversionupdator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
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
import java.util.Vector;

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
        try(Repository repository = new FileRepositoryBuilder().readEnvironment().findGitDir(getProject().getRootDir()).build();
            Git git = new Git(repository)) {
            String branchName = "update-library-" + now.format(DateTimeFormatter.ofPattern("uuuu-MM-dd"));
            git.checkout().setCreateBranch(true).setName(branchName).call();
            git.add().addFilepattern("build.gradle").call();
            git.commit().setMessage("Update library " + now.format(DateTimeFormatter.ofPattern("uuuu/MM/dd"))).call();
            git.push().setTransportConfigCallback(new SshTransportConfigCallback()).call();

            return branchName;
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private void createPullRequest(DependencyReport report, String branch, LocalDate now) throws IOException {
        URL url = URI.create(githubPage.replaceAll("github.com", "api.github.com/repos") + "/pulls").toURL();
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

    private static class SshTransportConfigCallback implements TransportConfigCallback {

        private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch jSch = super.createDefaultJSch(fs);
                jSch.setConfigRepository(OpenSshConfig.get(fs));
                return jSch;
            }
        };

        @Override
        public void configure(Transport transport) {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        }

    }
}
