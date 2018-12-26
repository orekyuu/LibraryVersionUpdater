package net.orekyuu.libraryversionupdator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DependencyList {
    private final List<Dependency> dependencies;
    private final int count;

    @JsonCreator
    public DependencyList(@JsonProperty("dependencies") List<Dependency> dependencies, @JsonProperty("count") int count) {
        this.dependencies = dependencies;
        this.count = count;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public int getCount() {
        return count;
    }
}
