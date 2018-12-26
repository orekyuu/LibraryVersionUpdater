package net.orekyuu.libraryversionupdator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DependencyReport {
    private final DependencyList outdated;
    private final DependencyList current;

    @JsonCreator
    public DependencyReport(@JsonProperty("outdated") DependencyList outdated, @JsonProperty("current") DependencyList current) {
        this.outdated = outdated;
        this.current = current;
    }

    public DependencyList getOutdated() {
        return outdated;
    }

    public DependencyList getCurrent() {
        return current;
    }

    public String pullRequestTitle(LocalDate now) {
        return "[" + now.format(DateTimeFormatter.ofPattern("uuuu-MM-dd"))+ "] Update library";
    }

    public String pullRequestMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Updated libraries\\n");
        builder.append("|Name|Version|\\n");
        builder.append("|:--:|:--:|\\n");

        for (Dependency dependency : outdated.getDependencies()) {
            String oldVersion = dependency.getVersion();
            String newVersion = dependency.getAvailable().getRelease();
            String name = dependency.getGroup() + ":" + dependency.getName();
            String projectUrl = dependency.getProjectUrl();
            if (projectUrl == null) {
                builder.append("|").append(name).append("|").append(oldVersion).append(" -> ").append(newVersion).append("|\\n");
            } else {
                builder.append("|[").append(name).append("](").append(projectUrl).append(")|").append(oldVersion).append(" -> ").append(newVersion).append("|\\n");
            }
        }
        return builder.toString();
    }
}
