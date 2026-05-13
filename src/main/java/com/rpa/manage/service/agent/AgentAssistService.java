package com.rpa.manage.service.agent;

import com.rpa.manage.domain.dto.agent.AiModelConfigRequest;
import com.rpa.manage.domain.dto.agent.AiModelConfigResponse;
import com.rpa.manage.domain.dto.agent.AgentAssistRequest;
import com.rpa.manage.domain.dto.agent.AgentAssistResponse;
import com.rpa.manage.domain.dto.agent.AgentQuotaCalculateRequest;
import com.rpa.manage.domain.dto.agent.AgentQuotaCalculateResponse;

public interface AgentAssistService {

    AgentAssistResponse assist(AgentAssistRequest request);

    AgentQuotaCalculateResponse calculateQuota(AgentQuotaCalculateRequest request);

    AiModelConfigResponse getModelConfig();

    AiModelConfigResponse saveModelConfig(AiModelConfigRequest request);
}
