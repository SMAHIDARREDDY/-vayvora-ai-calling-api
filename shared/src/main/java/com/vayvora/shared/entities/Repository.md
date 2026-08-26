# Repository Interfaces

## User Repository
- findByEmail(String email): Optional<User>
- findById(Long id): Optional<User>
- findByOrganizationId(Long orgId, Pageable): Page<User>
- save(User): User

## AI Agent Repository
- findById(Long id): Optional<AiAgent>
- findByOrganizationId(Long orgId, Pageable): Page<AiAgent>
- findByOrganizationIdAndStatus(Long orgId, String status, Pageable): Page<AiAgent>

## Campaign Repository
- findById(Long id): Optional<Campaign>
- findByOrganizationId(Long orgId, Pageable): Page<Campaign>
- findByOrganizationIdAndStatus(Long orgId, String status, Pageable): Page<Campaign>
- findByIdAndOrganizationId(Long id, Long orgId): Optional<Campaign>

## Call Repository
- findById(String id): Optional<Call>
- findByOrganizationId(Long orgId, Pageable): Page<Call>
- findByOrganizationIdAndStatus(Long orgId, String status, Pageable): Page<Call>
- findByOrganizationIdAndAgentId(Long orgId, Long agentId, Pageable): Page<Call>
- findByOrganizationIdAndCampaignId(Long orgId, Long campaignId, Pageable): Page<Call>

## Contact Repository
- findById(Long id): Optional<Contact>
- findByOrganizationId(Long orgId, Pageable): Page<Contact>
- findByPhoneNumber(String phone): Optional<Contact>

## Knowledge Base Repository
- findById(Long id): Optional<KnowledgeBase>
- findByOrganizationId(Long orgId, Pageable): Page<KnowledgeBase>

## Knowledge Document Repository
- findById(Long id): Optional<KnowledgeDocument>
- findByKnowledgeBaseId(Long kbId, Pageable): Page<KnowledgeDocument>

## Knowledge Chunk Repository
- findById(Long id): Optional<KnowledgeChunk>
- findByDocumentId(Long docId): List<KnowledgeChunk>
- findByEmbeddingsSimilarTo(Vector embedding): List<KnowledgeChunk>
