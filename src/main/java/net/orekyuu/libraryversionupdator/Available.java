package net.orekyuu.libraryversionupdator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Available {
    private final String release;
    private final String milestone;
    private final String integration;

    @JsonCreator
    public Available(@JsonProperty("release") String release, @JsonProperty("milestone") String milestone, @JsonProperty("integration") String integration) {
        this.release = release;
        this.milestone = milestone;
        this.integration = integration;
    }

    public String getRelease() {
        return release;
    }

    public String getMilestone() {
        return milestone;
    }

    public String getIntegration() {
        return integration;
    }
}
