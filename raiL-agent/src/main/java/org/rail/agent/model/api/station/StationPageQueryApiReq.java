package org.rail.agent.model.api.station;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.rail.agent.model.common.PageQuery;

/**
 * 远程站点查询接口请求DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StationPageQueryApiReq extends PageQuery {
    private Integer queryType;
    private String keyword;
}