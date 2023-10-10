package com.jfrog.ide.common.utils.usage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsageReport {
    @JsonProperty()
    private String productId;
    @JsonProperty()
    private String accountId;
    @JsonProperty()
    private String clientId;
    @JsonProperty()
    private String[] features;
}
