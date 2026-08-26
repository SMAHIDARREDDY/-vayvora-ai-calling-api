#!/bin/bash

# Create comprehensive Spring Boot microservices

# Auth Service
mkdir -p auth-service/src/main/java/com/vayvora/authservice/{config,controller,service,repository,entity,dto,security}
mkdir -p auth-service/src/main/resources

# Agent Service  
mkdir -p agent-service/src/main/java/com/vayvora/agentservice/{config,controller,service,repository,entity,dto}
mkdir -p agent-service/src/main/resources

# Call Service
mkdir -p call-service/src/main/java/com/vayvora/callservice/{config,controller,service,repository,entity,dto,kafka}
mkdir -p call-service/src/main/resources

# Campaign Service
mkdir -p campaign-service/src/main/java/com/vayvora/campaignservice/{config,controller,service,repository,entity,dto}
mkdir -p campaign-service/src/main/resources

# Analytics Service
mkdir -p analytics-service/src/main/java/com/vayvora/analyticsservice/{config,controller,service,repository,entity,dto,kafka}
mkdir -p analytics-service/src/main/resources

# Knowledge Service
mkdir -p knowledge-service/src/main/java/com/vayvora/knowledgeservice/{config,controller,service,repository,entity,dto}
mkdir -p knowledge-service/src/main/resources

# API Gateway
mkdir -p api-gateway/src/main/java/com/vayvora/apigateway/{config,filter,dto}
mkdir -p api-gateway/src/main/resources

echo "All service directories created"
