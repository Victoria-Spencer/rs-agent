package org.rail.agent.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class AiStationQueryReq {
    @JsonProperty(required = true)
    @JsonPropertyDescription("站点关键词：城市名、站点名、拼音，例如：厦门、上海、xiamen")
    private String keyword;
}