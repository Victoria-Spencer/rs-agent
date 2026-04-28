package org.rail.agent.model.api.station;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationQueryResp {
    private String name;
    private String code;
    private String spell;
}