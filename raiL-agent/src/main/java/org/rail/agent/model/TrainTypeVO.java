package org.rail.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainTypeVO {

    private Integer typeId;
    private String typeName;
    private String typeCode;
}
