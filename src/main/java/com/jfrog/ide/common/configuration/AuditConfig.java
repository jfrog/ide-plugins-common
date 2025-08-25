package com.jfrog.ide.common.configuration;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class AuditConfig {
    // Getters
    private final List<String> scannedDirectories;
    private final String serverId;
    private final List<String> excludedPattern;
    private final List<String> extraArgs;
    private final Map<String, String> envVars;

    private AuditConfig(Builder builder) {
        this.scannedDirectories = builder.scannedDirectories;
        this.serverId = builder.serverId;
        this.excludedPattern = builder.excludedPattern;
        this.extraArgs = builder.extraArgs;
        this.envVars = builder.envVars;
    }

    public static class Builder {
        private List<String> scannedDirectories;
        private String serverId;
        private List<String> excludedPattern;
        private List<String> extraArgs;
        private Map<String, String> envVars;

        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder scannedDirectories(List<String> scannedDirectories) {
            this.scannedDirectories = scannedDirectories;
            return this;
        }

        public Builder excludedPattern(List<String> excludedPattern) {
            this.excludedPattern = excludedPattern;
            return this;
        }

        public Builder extraArgs(List<String> extraArgs) {
            this.extraArgs = extraArgs;
            return this;
        }

        public Builder envVars(Map<String, String> envVars) {
            this.envVars = envVars;
            return this;
        }

        public AuditConfig build() {
            return new AuditConfig(this);
        }
    }

}
