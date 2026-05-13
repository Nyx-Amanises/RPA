package com.rpa.manage.controller.agent;

import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.agent.AiModelConfigRequest;
import com.rpa.manage.domain.dto.agent.AiModelConfigResponse;
import com.rpa.manage.domain.dto.agent.AgentAssistRequest;
import com.rpa.manage.domain.dto.agent.AgentAssistResponse;
import com.rpa.manage.domain.dto.agent.AgentQuotaCalculateRequest;
import com.rpa.manage.domain.dto.agent.AgentQuotaCalculateResponse;
import com.rpa.manage.service.agent.AgentAssistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Agent 辅助", description = "公式草稿、公式检查、额度解释")
@RestController
@RequestMapping("/api/v1/agent-assist")
@RequiredArgsConstructor
public class AgentAssistController {

    private final AgentAssistService agentAssistService;

    @Operation(summary = "执行 AI Agent 辅助任务")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<AgentAssistResponse> assist(@Valid @RequestBody AgentAssistRequest request) {
        return Result.success(agentAssistService.assist(request));
    }

    @Operation(summary = "执行 AI Agent 额度计算")
    @PostMapping("/quota-calculate")
    @PreAuthorize("isAuthenticated()")
    public Result<AgentQuotaCalculateResponse> calculateQuota(
            @Valid @RequestBody AgentQuotaCalculateRequest request
    ) {
        return Result.success(agentAssistService.calculateQuota(request));
    }

    @Operation(summary = "获取 AI 模型配置")
    @GetMapping("/model-config")
    @PreAuthorize("isAuthenticated()")
    public Result<AiModelConfigResponse> getModelConfig() {
        return Result.success(agentAssistService.getModelConfig());
    }

    @Operation(summary = "保存 AI 模型配置")
    @PutMapping("/model-config")
    @PreAuthorize("isAuthenticated()")
    public Result<AiModelConfigResponse> saveModelConfig(@Valid @RequestBody AiModelConfigRequest request) {
        return Result.success(agentAssistService.saveModelConfig(request));
    }
}
