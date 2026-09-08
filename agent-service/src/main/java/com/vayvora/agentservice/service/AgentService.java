package com.vayvora.agentservice.service;

import com.vayvora.agentservice.dto.AgentDtos.AgentConfigRequest;
import com.vayvora.agentservice.dto.AgentDtos.AgentResponse;
import com.vayvora.agentservice.dto.AgentDtos.AgentVersionResponse;
import com.vayvora.agentservice.dto.AgentDtos.ToolResponse;
import com.vayvora.agentservice.dto.AgentDtos.VoiceResponse;
import com.vayvora.shared.entity.Agents;
import com.vayvora.shared.enums.Enums.AgentStatus;
import com.vayvora.shared.enums.Enums.PromptVersionState;
import com.vayvora.shared.repository.Repositories.AiAgentRepository;
import com.vayvora.shared.repository.Repositories.AiAgentVersionRepository;
import com.vayvora.shared.repository.Repositories.AiToolRepository;
import com.vayvora.shared.repository.Repositories.AiVoiceRepository;
import com.vayvora.shared.tenant.TenantContext;
import com.vayvora.shared.web.Web.ConflictException;
import com.vayvora.shared.web.Web.NotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent lifecycle.
 *
 * <p>Configuration lives on {@code ai_agent_versions}, not on the agent row.
 * Editing a published agent creates a new draft version rather than mutating
 * the live one, so traffic keeps hitting the configuration that was actually
 * reviewed until someone publishes the replacement (spec §6).
 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AiAgentRepository agents;
    private final AiAgentVersionRepository versions;
    private final AiVoiceRepository voices;
    private final AiToolRepository tools;

    @Transactional(readOnly = true)
    public Page<AgentResponse> list(AgentStatus status, Pageable pageable) {
        String orgId = TenantContext.requireOrganizationId();
        Page<Agents.AiAgent> page = status == null
                ? agents.findByOrganizationId(orgId, pageable)
                : agents.findByOrganizationIdAndStatus(orgId, status, pageable);
        return page.map(agent -> AgentResponse.from(agent, currentVersion(agent)));
    }

    @Transactional(readOnly = true)
    public AgentResponse get(String agentId) {
        Agents.AiAgent agent = require(agentId);
        return AgentResponse.from(agent, currentVersion(agent));
    }

    @Transactional
    public AgentResponse create(AgentConfigRequest request) {
        String orgId = TenantContext.requireOrganizationId();

        Agents.AiAgent agent = agents.save(Agents.AiAgent.builder()
                .organizationId(orgId)
                .name(request.name())
                .agentType(request.agentType())
                .status(AgentStatus.DRAFT)
                .currentVersion(1)
                .createdBy(TenantContext.userId())
                .build());

        versions.save(buildVersion(orgId, agent.getId(), 1, request));
        return AgentResponse.from(agent, currentVersion(agent));
    }

    /**
     * Updates the agent's working configuration.
     *
     * <p>If the current version has already been published, a new draft version
     * is created; an unpublished draft is edited in place.
     */
    @Transactional
    public AgentResponse update(String agentId, AgentConfigRequest request) {
        String orgId = TenantContext.requireOrganizationId();
        Agents.AiAgent agent = require(agentId);

        agent.setName(request.name());
        agent.setAgentType(request.agentType());

        Agents.AiAgentVersion current = currentVersion(agent);
        if (current == null || current.getState() == PromptVersionState.PUBLISHED) {
            int next = agent.getCurrentVersion() == null ? 1 : agent.getCurrentVersion() + 1;
            agent.setCurrentVersion(next);
            versions.save(buildVersion(orgId, agent.getId(), next, request));
        } else {
            applyConfig(current, request);
            versions.save(current);
        }

        agents.save(agent);
        return AgentResponse.from(agent, currentVersion(agent));
    }

    /**
     * Makes the current version live.
     *
     * <p>Refuses an agent with no system prompt: publishing one would put an
     * agent on the phone with no instructions.
     */
    @Transactional
    public AgentResponse publish(String agentId) {
        Agents.AiAgent agent = require(agentId);
        Agents.AiAgentVersion version = currentVersion(agent);

        if (version == null) {
            throw new ConflictException("Agent has no configuration to publish");
        }
        if (version.getSystemPrompt() == null || version.getSystemPrompt().isBlank()) {
            throw new ConflictException(
                    "Agent cannot be published without a system prompt");
        }

        Instant now = Instant.now();
        version.setState(PromptVersionState.PUBLISHED);
        version.setPublishedAt(now);
        version.setPublishedBy(TenantContext.userId());
        versions.save(version);

        agent.setStatus(AgentStatus.PUBLISHED);
        agent.setPublishedVersion(version.getVersion());
        agent.setPublishedAt(now);
        agents.save(agent);

        return AgentResponse.from(agent, version);
    }

    /**
     * Takes an agent out of service.
     *
     * <p>Archiving does not delete history: past calls keep pointing at the
     * agent and version that handled them.
     */
    @Transactional
    public AgentResponse archive(String agentId) {
        Agents.AiAgent agent = require(agentId);
        agent.setStatus(AgentStatus.ARCHIVED);
        agents.save(agent);
        return AgentResponse.from(agent, currentVersion(agent));
    }

    @Transactional(readOnly = true)
    public List<AgentVersionResponse> versionHistory(String agentId) {
        require(agentId);
        return versions.findByAgentIdOrderByVersionDesc(agentId).stream()
                .map(AgentVersionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoiceResponse> availableVoices() {
        return voices.findByActiveTrue().stream().map(VoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ToolResponse> availableTools() {
        String orgId = TenantContext.requireOrganizationId();
        return tools.findByOrganizationIdOrOrganizationIdIsNull(orgId).stream()
                .filter(Agents.AiTool::isActive)
                .map(ToolResponse::from)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private Agents.AiAgent require(String agentId) {
        return agents.findByIdAndOrganizationId(
                        agentId, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Agent " + agentId + " not found"));
    }

    private Agents.AiAgentVersion currentVersion(Agents.AiAgent agent) {
        if (agent.getCurrentVersion() == null) {
            return null;
        }
        return versions.findByAgentIdAndVersion(agent.getId(), agent.getCurrentVersion())
                .orElse(null);
    }

    private Agents.AiAgentVersion buildVersion(String orgId, String agentId,
                                               int version, AgentConfigRequest request) {
        Agents.AiAgentVersion v = Agents.AiAgentVersion.builder()
                .organizationId(orgId)
                .agentId(agentId)
                .version(version)
                .state(PromptVersionState.DRAFT)
                .build();
        applyConfig(v, request);
        return v;
    }

    private void applyConfig(Agents.AiAgentVersion v, AgentConfigRequest r) {
        v.setLanguages(csv(r.languages()));
        v.setVoiceId(r.voiceId());
        v.setPersonality(csv(r.personality()));
        v.setSystemPrompt(r.systemPrompt());
        v.setKnowledgeBaseId(r.knowledgeBaseId());
        v.setEnabledTools(csv(r.enabledTools()));

        if (r.maxCallMinutes() != null) {
            v.setMaxCallMinutes(r.maxCallMinutes());
        }
        if (r.callingWindowStart() != null) {
            v.setCallingWindowStart(r.callingWindowStart());
        }
        if (r.callingWindowEnd() != null) {
            v.setCallingWindowEnd(r.callingWindowEnd());
        }
        if (r.handoffOnRequest() != null) {
            v.setHandoffOnRequest(r.handoffOnRequest());
        }
        if (r.handoffOnNegativeSentiment() != null) {
            v.setHandoffOnNegativeSentiment(r.handoffOnNegativeSentiment());
        }
        if (r.handoffOnBillingDispute() != null) {
            v.setHandoffOnBillingDispute(r.handoffOnBillingDispute());
        }
    }

    private static String csv(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }
}
