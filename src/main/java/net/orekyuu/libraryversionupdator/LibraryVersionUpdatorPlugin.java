package net.orekyuu.libraryversionupdator;

import com.github.benmanes.gradle.versions.VersionsPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import se.patrikerdes.UseLatestVersionsPlugin;

public class LibraryVersionUpdatorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(VersionsPlugin.class);
        project.getPluginManager().apply(UseLatestVersionsPlugin.class);

        project.getTasks().create("createLibraryUpdatePR", UdateLibPullRequest.class, (task) -> {
            task.setGithubAccessToken("default");
            task.setGithubPage("default");
            task.dependsOn("useLatestVersions");
        });
    }
}
