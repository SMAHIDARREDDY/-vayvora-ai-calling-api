-- Reference data: the platform-level rows every organization draws on.
--
-- These are not tenant data. Voices, tools, plans and permissions are defined
-- once for the whole platform, so they are seeded as part of the schema rather
-- than created per organization.
--
-- Prices below are the ILLUSTRATIVE figures from the product documentation
-- (§22), which states explicitly that they are not final commercial pricing.
-- They must not be quoted to customers.

-- ---------------------------------------------------------------------------
-- Permissions (§5)
-- ---------------------------------------------------------------------------

INSERT INTO permissions (id, code, description, created_at, updated_at) VALUES
    ('perm-0001', 'organizations.create',  'Create organizations',            NOW(), NOW()),
    ('perm-0002', 'subscriptions.manage',  'Manage subscriptions',            NOW(), NOW()),
    ('perm-0003', 'agents.create',         'Create AI agents',                NOW(), NOW()),
    ('perm-0004', 'agents.publish',        'Publish AI agent versions',       NOW(), NOW()),
    ('perm-0005', 'knowledge.upload',      'Upload knowledge documents',      NOW(), NOW()),
    ('perm-0006', 'campaigns.create',      'Create campaigns',                NOW(), NOW()),
    ('perm-0007', 'campaigns.manage',      'Start, pause and edit campaigns', NOW(), NOW()),
    ('perm-0008', 'calls.view',            'View calls',                      NOW(), NOW()),
    ('perm-0009', 'calls.handle_transfer', 'Handle transferred calls',        NOW(), NOW()),
    ('perm-0010', 'analytics.view',        'View analytics',                  NOW(), NOW()),
    ('perm-0011', 'leads.manage',          'Manage leads',                    NOW(), NOW()),
    ('perm-0012', 'users.invite',          'Add users to the organization',   NOW(), NOW()),
    ('perm-0013', 'billing.manage',        'Manage billing',                  NOW(), NOW()),
    ('perm-0014', 'apikeys.create',        'Create API keys',                 NOW(), NOW()),
    ('perm-0015', 'webhooks.configure',    'Configure webhooks',              NOW(), NOW()),
    ('perm-0016', 'integrations.manage',   'Manage integrations',             NOW(), NOW()),
    ('perm-0017', 'phone_numbers.manage',  'Purchase and assign numbers',     NOW(), NOW()),
    ('perm-0018', 'audit.view',            'View the audit log',              NOW(), NOW());

-- ---------------------------------------------------------------------------
-- Voices (§6, §19)
-- ---------------------------------------------------------------------------

INSERT INTO ai_voices (id, code, display_name, gender, locale, provider, active,
                       created_at, updated_at) VALUES
    ('voice-001', 'en_in_female_01', 'Aria — Indian English',  'FEMALE', 'en-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-002', 'en_in_male_01',   'Arjun — Indian English',  'MALE',   'en-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-003', 'hi_in_female_01', 'Meera — Hindi',           'FEMALE', 'hi-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-004', 'hi_in_male_01',   'Rohan — Hindi',           'MALE',   'hi-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-005', 'te_in_female_01', 'Lakshmi — Telugu',        'FEMALE', 'te-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-006', 'te_in_male_01',   'Karthik — Telugu',        'MALE',   'te-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-007', 'ta_in_female_01', 'Divya — Tamil',           'FEMALE', 'ta-IN', 'platform', TRUE, NOW(), NOW()),
    ('voice-008', 'kn_in_female_01', 'Ananya — Kannada',        'FEMALE', 'kn-IN', 'platform', TRUE, NOW(), NOW());

-- ---------------------------------------------------------------------------
-- Platform tools (§12)
--
-- organization_id NULL means the tool is available to every organization. An
-- agent can only invoke a tool that has been registered here and enabled on
-- its configuration.
-- ---------------------------------------------------------------------------

INSERT INTO ai_tools (id, organization_id, code, display_name, description,
                      parameters_schema, requires_confirmation, active,
                      created_at, updated_at) VALUES
    ('tool-001', NULL, 'book_appointment', 'Book appointment',
     'Check availability and create a booking for the customer',
     '{"type":"object","properties":{"date":{"type":"string","format":"date"},"time":{"type":"string"},"durationMinutes":{"type":"integer"}},"required":["date","time"]}',
     TRUE, TRUE, NOW(), NOW()),

    ('tool-002', NULL, 'check_availability', 'Check availability',
     'Look up free slots before offering a time',
     '{"type":"object","properties":{"date":{"type":"string","format":"date"}},"required":["date"]}',
     FALSE, TRUE, NOW(), NOW()),

    ('tool-003', NULL, 'reschedule_appointment', 'Reschedule appointment',
     'Move an existing booking to a new time',
     '{"type":"object","properties":{"appointmentId":{"type":"string"},"date":{"type":"string","format":"date"},"time":{"type":"string"}},"required":["appointmentId","date","time"]}',
     TRUE, TRUE, NOW(), NOW()),

    ('tool-004', NULL, 'cancel_appointment', 'Cancel appointment',
     'Cancel an existing booking',
     '{"type":"object","properties":{"appointmentId":{"type":"string"}},"required":["appointmentId"]}',
     TRUE, TRUE, NOW(), NOW()),

    ('tool-005', NULL, 'lookup_customer', 'Look up customer',
     'Retrieve an existing account or order so the customer need not repeat it',
     '{"type":"object","properties":{"phone":{"type":"string"},"email":{"type":"string"}}}',
     FALSE, TRUE, NOW(), NOW()),

    ('tool-006', NULL, 'create_lead', 'Create lead',
     'Write a qualified lead into the CRM',
     '{"type":"object","properties":{"requirement":{"type":"string"},"budget":{"type":"string"},"timeline":{"type":"string"}}}',
     FALSE, TRUE, NOW(), NOW()),

    ('tool-007', NULL, 'create_ticket', 'Create support ticket',
     'Raise a ticket for an issue that cannot be resolved on the call',
     '{"type":"object","properties":{"subject":{"type":"string"},"description":{"type":"string"},"priority":{"type":"string"}},"required":["subject"]}',
     FALSE, TRUE, NOW(), NOW()),

    ('tool-008', NULL, 'send_followup', 'Send follow-up',
     'Trigger a follow-up email or WhatsApp message',
     '{"type":"object","properties":{"channel":{"type":"string"},"template":{"type":"string"}},"required":["channel"]}',
     FALSE, TRUE, NOW(), NOW()),

    ('tool-009', NULL, 'transfer_to_human', 'Transfer to human',
     'Hand the call to an available employee',
     '{"type":"object","properties":{"reason":{"type":"string"},"department":{"type":"string"}},"required":["reason"]}',
     FALSE, TRUE, NOW(), NOW()),

    ('tool-010', NULL, 'lookup_subscription', 'Look up subscription',
     'Retrieve the customer''s current plan and renewal date',
     '{"type":"object","properties":{"accountId":{"type":"string"}}}',
     FALSE, TRUE, NOW(), NOW());

-- ---------------------------------------------------------------------------
-- Telephony providers (§8)
-- ---------------------------------------------------------------------------

INSERT INTO telephony_providers (id, code, display_name, gateway_uri, active,
                                 created_at, updated_at) VALUES
    ('prov-001', 'sip_primary', 'Primary SIP Gateway', 'sip://sip-gateway.internal:5060', TRUE, NOW(), NOW());

-- ---------------------------------------------------------------------------
-- Plans (§22)
--
-- ILLUSTRATIVE PRICING — the product documentation states these are example
-- figures, not recommended or confirmed commercial pricing. Amounts are in
-- paise: 99900 = Rs 999.00.
-- ---------------------------------------------------------------------------

INSERT INTO plans (id, code, name, monthly_price_minor, currency,
                   included_minutes, max_agents, max_phone_numbers,
                   included_storage_gb, overage_per_minute_minor, active,
                   created_at, updated_at) VALUES
    ('plan-starter',    'STARTER',    'Starter',      99900, 'INR',   500,    1,   1,   5,  300, TRUE, NOW(), NOW()),
    ('plan-business',   'BUSINESS',   'Business',    499900, 'INR',  3000,    5,  10,  50,  250, TRUE, NOW(), NOW()),
    -- Enterprise is custom-priced, so monthly_price_minor is NULL and limits
    -- are left open rather than set to an arbitrary ceiling.
    ('plan-enterprise', 'ENTERPRISE', 'Enterprise',    NULL, 'INR',  NULL, NULL, NULL, NULL, NULL, TRUE, NOW(), NOW());
