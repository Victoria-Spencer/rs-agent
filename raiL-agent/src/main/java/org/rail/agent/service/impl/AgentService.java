package org.rail.agent.service.impl;

import org.rail.agent.pojo.dto.AgentRequestDTO;
import org.rail.agent.pojo.dto.AgentResponseVO;

public interface AgentService {
    AgentResponseVO chat(AgentRequestDTO requestDTO);
}
