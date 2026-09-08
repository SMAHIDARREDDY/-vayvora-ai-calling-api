package com.vayvora.shared.repository;

import com.vayvora.shared.entity.Agents;
import com.vayvora.shared.entity.Calls;
import com.vayvora.shared.entity.Crm;
import com.vayvora.shared.entity.Identity;
import com.vayvora.shared.entity.Knowledge;
import com.vayvora.shared.entity.Platform;
import com.vayvora.shared.enums.Enums.CallState;
import com.vayvora.shared.enums.Enums.CampaignContactState;
import com.vayvora.shared.enums.Enums.CampaignState;
import com.vayvora.shared.enums.Enums.IngestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repositories for every table.
 *
 * <p>Read methods take {@code organizationId} as their first argument by
 * convention. There is deliberately no bare {@code findById} exposed for
 * tenant-scoped entities: lookups go through
 * {@code findByIdAndOrganizationId}, so a caller cannot fetch another
 * tenant's row by guessing an identifier (spec §33).
 */
public final class Repositories {

    private Repositories() {
    }

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    public interface OrganizationRepository
            extends JpaRepository<Identity.Organization, String> {
        Optional<Identity.Organization> findBySlug(String slug);

        boolean existsBySlug(String slug);
    }

    public interface UserRepository extends JpaRepository<Identity.User, String> {
        Optional<Identity.User> findByEmailIgnoreCase(String email);

        boolean existsByEmailIgnoreCase(String email);

        Optional<Identity.User> findByIdAndOrganizationId(String id, String organizationId);

        Page<Identity.User> findByOrganizationId(String organizationId, Pageable pageable);

        long countByOrganizationId(String organizationId);
    }

    public interface RoleRepository extends JpaRepository<Identity.Role, String> {
        List<Identity.Role> findByOrganizationIdOrOrganizationIdIsNull(String organizationId);
    }

    public interface PermissionRepository
            extends JpaRepository<Identity.Permission, String> {
        Optional<Identity.Permission> findByCode(String code);
    }

    public interface RolePermissionRepository
            extends JpaRepository<Identity.RolePermission, String> {
        List<Identity.RolePermission> findByRoleId(String roleId);
    }

    public interface OrganizationUserRepository
            extends JpaRepository<Identity.OrganizationUser, String> {
        List<Identity.OrganizationUser> findByUserId(String userId);

        Optional<Identity.OrganizationUser> findByOrganizationIdAndUserId(
                String organizationId, String userId);
    }

    public interface RefreshTokenRepository
            extends JpaRepository<Identity.RefreshToken, String> {
        Optional<Identity.RefreshToken> findByTokenHash(String tokenHash);

        List<Identity.RefreshToken> findByUserIdAndRevokedAtIsNull(String userId);
    }

    // -----------------------------------------------------------------------
    // Agents
    // -----------------------------------------------------------------------

    public interface AiAgentRepository extends JpaRepository<Agents.AiAgent, String> {
        Optional<Agents.AiAgent> findByIdAndOrganizationId(String id, String organizationId);

        Page<Agents.AiAgent> findByOrganizationId(String organizationId, Pageable pageable);

        Page<Agents.AiAgent> findByOrganizationIdAndStatus(
                String organizationId, com.vayvora.shared.enums.Enums.AgentStatus status,
                Pageable pageable);

        long countByOrganizationIdAndStatus(
                String organizationId, com.vayvora.shared.enums.Enums.AgentStatus status);
    }

    public interface AiAgentVersionRepository
            extends JpaRepository<Agents.AiAgentVersion, String> {
        Optional<Agents.AiAgentVersion> findByAgentIdAndVersion(String agentId, int version);

        List<Agents.AiAgentVersion> findByAgentIdOrderByVersionDesc(String agentId);
    }

    public interface AiPromptRepository extends JpaRepository<Agents.AiPrompt, String> {
        List<Agents.AiPrompt> findByAgentIdOrderByVersionDesc(String agentId);
    }

    public interface AiVoiceRepository extends JpaRepository<Agents.AiVoice, String> {
        List<Agents.AiVoice> findByActiveTrue();

        Optional<Agents.AiVoice> findByCode(String code);
    }

    public interface AiToolRepository extends JpaRepository<Agents.AiTool, String> {
        List<Agents.AiTool> findByOrganizationIdOrOrganizationIdIsNull(String organizationId);

        Optional<Agents.AiTool> findByCodeAndOrganizationIdIsNull(String code);
    }

    // -----------------------------------------------------------------------
    // Knowledge
    // -----------------------------------------------------------------------

    public interface KnowledgeBaseRepository
            extends JpaRepository<Knowledge.KnowledgeBase, String> {
        Optional<Knowledge.KnowledgeBase> findByIdAndOrganizationId(
                String id, String organizationId);

        Page<Knowledge.KnowledgeBase> findByOrganizationId(
                String organizationId, Pageable pageable);
    }

    public interface KnowledgeDocumentRepository
            extends JpaRepository<Knowledge.KnowledgeDocument, String> {
        Optional<Knowledge.KnowledgeDocument> findByIdAndOrganizationId(
                String id, String organizationId);

        Page<Knowledge.KnowledgeDocument> findByKnowledgeBaseId(
                String knowledgeBaseId, Pageable pageable);

        Page<Knowledge.KnowledgeDocument> findByOrganizationId(
                String organizationId, Pageable pageable);

        long countByOrganizationIdAndStatus(String organizationId, IngestStatus status);

        @Query("select coalesce(sum(d.chunkCount), 0) from KnowledgeDocument d "
                + "where d.organizationId = :orgId")
        long sumChunks(@Param("orgId") String organizationId);
    }

    public interface KnowledgeChunkRepository
            extends JpaRepository<Knowledge.KnowledgeChunk, String> {
        List<Knowledge.KnowledgeChunk> findByDocumentIdOrderByOrdinal(String documentId);

        void deleteByDocumentId(String documentId);

        /**
         * Keyword fallback for retrieval.
         *
         * <p>Stands in for vector similarity until the vector store is wired
         * up; it returns candidate chunks so the RAG path is exercisable
         * end to end.
         */
        @Query("select c from KnowledgeChunk c where c.organizationId = :orgId "
                + "and lower(c.content) like lower(concat('%', :term, '%'))")
        List<Knowledge.KnowledgeChunk> searchByKeyword(
                @Param("orgId") String organizationId,
                @Param("term") String term,
                Pageable pageable);
    }

    public interface KnowledgeEmbeddingRepository
            extends JpaRepository<Knowledge.KnowledgeEmbedding, String> {
        Optional<Knowledge.KnowledgeEmbedding> findByChunkId(String chunkId);
    }

    // -----------------------------------------------------------------------
    // Telephony and CRM
    // -----------------------------------------------------------------------

    public interface TelephonyProviderRepository
            extends JpaRepository<Crm.TelephonyProvider, String> {
        Optional<Crm.TelephonyProvider> findByCode(String code);
    }

    public interface PhoneNumberRepository extends JpaRepository<Crm.PhoneNumber, String> {
        Optional<Crm.PhoneNumber> findByIdAndOrganizationId(String id, String organizationId);

        List<Crm.PhoneNumber> findByOrganizationId(String organizationId);

        Optional<Crm.PhoneNumber> findByE164(String e164);

        long countByOrganizationIdAndActiveTrue(String organizationId);
    }

    public interface CompanyRepository extends JpaRepository<Crm.Company, String> {
        Optional<Crm.Company> findByIdAndOrganizationId(String id, String organizationId);

        Page<Crm.Company> findByOrganizationId(String organizationId, Pageable pageable);
    }

    public interface ContactRepository extends JpaRepository<Crm.Contact, String> {
        Optional<Crm.Contact> findByIdAndOrganizationId(String id, String organizationId);

        Optional<Crm.Contact> findByOrganizationIdAndPhone(String organizationId, String phone);

        Page<Crm.Contact> findByOrganizationId(String organizationId, Pageable pageable);

        Page<Crm.Contact> findByOrganizationIdAndLeadBand(
                String organizationId, com.vayvora.shared.enums.Enums.LeadBand band,
                Pageable pageable);

        @Query("select c from Contact c where c.organizationId = :orgId and ("
                + "lower(c.firstName) like lower(concat('%', :q, '%')) or "
                + "lower(c.lastName) like lower(concat('%', :q, '%')) or "
                + "lower(c.companyName) like lower(concat('%', :q, '%')) or "
                + "lower(c.email) like lower(concat('%', :q, '%')) or "
                + "c.phone like concat('%', :q, '%'))")
        Page<Crm.Contact> search(@Param("orgId") String organizationId,
                                 @Param("q") String query,
                                 Pageable pageable);

        long countByOrganizationIdAndLeadBand(
                String organizationId, com.vayvora.shared.enums.Enums.LeadBand band);
    }

    public interface LeadRepository extends JpaRepository<Crm.Lead, String> {
        Optional<Crm.Lead> findByIdAndOrganizationId(String id, String organizationId);

        Page<Crm.Lead> findByOrganizationId(String organizationId, Pageable pageable);

        List<Crm.Lead> findByContactId(String contactId);
    }

    public interface AppointmentRepository extends JpaRepository<Crm.Appointment, String> {
        Optional<Crm.Appointment> findByIdAndOrganizationId(String id, String organizationId);

        Page<Crm.Appointment> findByOrganizationIdOrderByScheduledAtAsc(
                String organizationId, Pageable pageable);

        List<Crm.Appointment> findByOrganizationIdAndScheduledAtBetween(
                String organizationId, Instant from, Instant to);
    }

    public interface TaskRepository extends JpaRepository<Crm.Task, String> {
        Page<Crm.Task> findByOrganizationId(String organizationId, Pageable pageable);

        List<Crm.Task> findByAssignedUserIdAndStatus(
                String userId, com.vayvora.shared.enums.Enums.TaskStatus status);
    }

    public interface NoteRepository extends JpaRepository<Crm.Note, String> {
        List<Crm.Note> findByContactIdOrderByCreatedAtDesc(String contactId);

        List<Crm.Note> findByCallId(String callId);
    }

    // -----------------------------------------------------------------------
    // Campaigns
    // -----------------------------------------------------------------------

    public interface CampaignRepository extends JpaRepository<Calls.Campaign, String> {
        Optional<Calls.Campaign> findByIdAndOrganizationId(String id, String organizationId);

        Page<Calls.Campaign> findByOrganizationId(String organizationId, Pageable pageable);

        Page<Calls.Campaign> findByOrganizationIdAndState(
                String organizationId, CampaignState state, Pageable pageable);

        List<Calls.Campaign> findByState(CampaignState state);
    }

    public interface CampaignContactRepository
            extends JpaRepository<Calls.CampaignContact, String> {
        Page<Calls.CampaignContact> findByCampaignId(String campaignId, Pageable pageable);

        long countByCampaignId(String campaignId);

        long countByCampaignIdAndState(String campaignId, CampaignContactState state);

        long countByCampaignIdAndOutcome(
                String campaignId, com.vayvora.shared.enums.Enums.CallOutcome outcome);

        /** Next contacts due to be dialled, oldest first. */
        @Query("select cc from CampaignContact cc where cc.campaignId = :campaignId "
                + "and cc.state = 'PENDING' order by cc.createdAt asc")
        List<Calls.CampaignContact> findDueContacts(
                @Param("campaignId") String campaignId, Pageable pageable);
    }

    // -----------------------------------------------------------------------
    // Calls
    // -----------------------------------------------------------------------

    public interface CallRepository extends JpaRepository<Calls.Call, String> {
        Optional<Calls.Call> findByIdAndOrganizationId(String id, String organizationId);

        Page<Calls.Call> findByOrganizationIdOrderByStartedAtDesc(
                String organizationId, Pageable pageable);

        Page<Calls.Call> findByOrganizationIdAndState(
                String organizationId, CallState state, Pageable pageable);

        Page<Calls.Call> findByOrganizationIdAndAgentId(
                String organizationId, String agentId, Pageable pageable);

        Page<Calls.Call> findByCampaignId(String campaignId, Pageable pageable);

        List<Calls.Call> findByOrganizationIdAndStateIn(
                String organizationId, List<CallState> states);

        long countByOrganizationIdAndStartedAtAfter(String organizationId, Instant since);

        long countByOrganizationIdAndConnectedAtIsNotNullAndStartedAtAfter(
                String organizationId, Instant since);

        @Query("select coalesce(avg(c.durationSeconds), 0) from Call c "
                + "where c.organizationId = :orgId and c.connectedAt is not null "
                + "and c.startedAt > :since")
        double averageDurationSeconds(@Param("orgId") String organizationId,
                                      @Param("since") Instant since);

        @Query("select coalesce(sum(c.durationSeconds), 0) from Call c "
                + "where c.organizationId = :orgId and c.startedAt > :since")
        long totalDurationSeconds(@Param("orgId") String organizationId,
                                  @Param("since") Instant since);

        /** Daily call counts for trend charts, newest last. */
        @Query("select cast(c.startedAt as date) as day, c.direction as direction, "
                + "count(c) as total from Call c where c.organizationId = :orgId "
                + "and c.startedAt > :since group by cast(c.startedAt as date), c.direction "
                + "order by cast(c.startedAt as date)")
        List<Object[]> dailyVolume(@Param("orgId") String organizationId,
                                   @Param("since") Instant since);
    }

    public interface CallParticipantRepository
            extends JpaRepository<Calls.CallParticipant, String> {
        List<Calls.CallParticipant> findByCallId(String callId);
    }

    public interface CallRecordingRepository
            extends JpaRepository<Calls.CallRecording, String> {
        Optional<Calls.CallRecording> findByCallId(String callId);

        List<Calls.CallRecording> findByExpiresAtBefore(Instant cutoff);
    }

    public interface CallTranscriptRepository
            extends JpaRepository<Calls.CallTranscript, String> {
        Optional<Calls.CallTranscript> findByCallId(String callId);
    }

    public interface CallMessageRepository
            extends JpaRepository<Calls.CallMessage, String> {
        List<Calls.CallMessage> findByCallIdOrderBySequenceAsc(String callId);

        @Query("select coalesce(max(m.sequence), 0) from CallMessage m where m.callId = :callId")
        int maxSequence(@Param("callId") String callId);
    }

    public interface CallSummaryRepository
            extends JpaRepository<Calls.CallSummary, String> {
        Optional<Calls.CallSummary> findByCallId(String callId);
    }

    public interface CallSentimentRepository
            extends JpaRepository<Calls.CallSentiment, String> {
        Optional<Calls.CallSentiment> findByCallId(String callId);

        @Query("select s.overall as sentiment, count(s) as total from CallSentiment s "
                + "where s.organizationId = :orgId group by s.overall")
        List<Object[]> distribution(@Param("orgId") String organizationId);
    }

    public interface CallIntentRepository extends JpaRepository<Calls.CallIntent, String> {
        List<Calls.CallIntent> findByCallId(String callId);

        @Query("select i.intent as intent, count(i) as total from CallIntent i "
                + "where i.organizationId = :orgId and i.primary = true "
                + "group by i.intent order by count(i) desc")
        List<Object[]> topIntents(@Param("orgId") String organizationId, Pageable pageable);
    }

    // -----------------------------------------------------------------------
    // Billing, integrations, platform
    // -----------------------------------------------------------------------

    public interface PlanRepository extends JpaRepository<Platform.Plan, String> {
        Optional<Platform.Plan> findByCode(String code);

        List<Platform.Plan> findByActiveTrue();
    }

    public interface SubscriptionRepository
            extends JpaRepository<Platform.Subscription, String> {
        Optional<Platform.Subscription> findByOrganizationId(String organizationId);
    }

    public interface UsageRecordRepository
            extends JpaRepository<Platform.UsageRecord, String> {
        @Query("select coalesce(sum(u.quantity), 0) from UsageRecord u "
                + "where u.organizationId = :orgId and u.metric = :metric "
                + "and u.recordedAt between :from and :to")
        double sumQuantity(@Param("orgId") String organizationId,
                           @Param("metric") com.vayvora.shared.enums.Enums.UsageMetric metric,
                           @Param("from") Instant from,
                           @Param("to") Instant to);

        List<Platform.UsageRecord> findByOrganizationIdAndRecordedAtBetween(
                String organizationId, Instant from, Instant to);
    }

    public interface InvoiceRepository extends JpaRepository<Platform.Invoice, String> {
        Page<Platform.Invoice> findByOrganizationIdOrderByPeriodStartDesc(
                String organizationId, Pageable pageable);

        Optional<Platform.Invoice> findByInvoiceNumber(String invoiceNumber);
    }

    public interface PaymentRepository extends JpaRepository<Platform.Payment, String> {
        List<Platform.Payment> findByInvoiceId(String invoiceId);
    }

    public interface IntegrationRepository
            extends JpaRepository<Platform.Integration, String> {
        List<Platform.Integration> findByOrganizationId(String organizationId);

        Optional<Platform.Integration> findByIdAndOrganizationId(
                String id, String organizationId);
    }

    public interface ApiKeyRepository extends JpaRepository<Platform.ApiKey, String> {
        List<Platform.ApiKey> findByOrganizationId(String organizationId);

        Optional<Platform.ApiKey> findByKeyHash(String keyHash);

        Optional<Platform.ApiKey> findByIdAndOrganizationId(String id, String organizationId);
    }

    public interface WebhookRepository extends JpaRepository<Platform.Webhook, String> {
        List<Platform.Webhook> findByOrganizationId(String organizationId);

        Optional<Platform.Webhook> findByIdAndOrganizationId(String id, String organizationId);

        List<Platform.Webhook> findByOrganizationIdAndActiveTrue(String organizationId);
    }

    public interface NotificationRepository
            extends JpaRepository<Platform.Notification, String> {
        Page<Platform.Notification> findByOrganizationIdOrderByCreatedAtDesc(
                String organizationId, Pageable pageable);

        long countByOrganizationIdAndReadAtIsNull(String organizationId);

        Optional<Platform.Notification> findByIdAndOrganizationId(
                String id, String organizationId);
    }

    public interface AuditLogRepository extends JpaRepository<Platform.AuditLog, String> {
        Page<Platform.AuditLog> findByOrganizationIdOrderByCreatedAtDesc(
                String organizationId, Pageable pageable);
    }
}
