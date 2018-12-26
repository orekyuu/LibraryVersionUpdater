package net.orekyuu.libraryversionupdator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Dependency {
    private final String group;
    private final String version;
    private final String projectUrl;
    private final String name;
    private final Available available;

    @JsonCreator
    public Dependency(@JsonProperty("group") String group, @JsonProperty("version") String version, @JsonProperty("projectUrl")String projectUrl, @JsonProperty("name")String name, @JsonProperty("available")Available available) {
        this.group = group;
        this.version = version;
        this.projectUrl = projectUrl;
        this.name = name;
        this.available = available;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public String getName() {
        return name;
    }

    public Available getAvailable() {
        return available;
    }
}
