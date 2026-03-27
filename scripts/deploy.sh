#!/bin/bash
set -euo pipefail

# Deployment Script for Kamatera VPS
# This script is executed by GitHub Actions to deploy the application
# Location on VPS: /opt/prepeardness-api/deploy.sh

DEPLOY_DIR="/opt/prepeardness-api"
IMAGE_NAME="${IMAGE_NAME:-ghcr.io/kjetilminde/prepeardness-api}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
FULL_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
HEALTH_ENDPOINT="http://localhost:8080/actuator/health"
MAX_HEALTH_RETRIES=30
HEALTH_RETRY_DELAY=2

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Change to deployment directory
cd "$DEPLOY_DIR"

info "=================================="
info "Starting Deployment"
info "=================================="
info "Image: $FULL_IMAGE"
info "Deploy directory: $DEPLOY_DIR"
info "Time: $(date)"

# Check if .env file exists
if [[ ! -f .env ]]; then
    error ".env file not found. Please create it with required environment variables."
    exit 1
fi

# Login to GitHub Container Registry
step "Authenticating with GitHub Container Registry..."
if [[ -n "${GITHUB_TOKEN:-}" ]]; then
    echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_ACTOR" --password-stdin
    info "Successfully authenticated with ghcr.io"
else
    warn "GITHUB_TOKEN not provided, assuming already authenticated"
fi

# Tag current image as 'previous' for rollback
step "Creating backup tag for rollback..."
if docker image inspect "$FULL_IMAGE" &> /dev/null; then
    docker tag "$FULL_IMAGE" "${IMAGE_NAME}:previous" || warn "Failed to create backup tag"
    info "Backup tag created: ${IMAGE_NAME}:previous"
else
    warn "Current image not found, skipping backup"
fi

# Pull the latest image
step "Pulling latest Docker image..."
if ! docker pull "$FULL_IMAGE"; then
    error "Failed to pull Docker image: $FULL_IMAGE"
    exit 1
fi
info "Successfully pulled $FULL_IMAGE"

# Pull other required images
step "Pulling nginx image..."
docker compose pull nginx || warn "Failed to pull nginx image"

# Get current container ID for health check comparison
CURRENT_CONTAINER=$(docker compose ps -q app 2>/dev/null || echo "")

# Perform zero-downtime deployment
step "Deploying application (zero-downtime)..."
if ! docker compose up -d --no-deps --build app; then
    error "Failed to start new container"
    step "Attempting rollback..."
    if docker tag "${IMAGE_NAME}:previous" "$FULL_IMAGE" 2>/dev/null; then
        docker compose up -d --no-deps app
        error "Deployment failed, rolled back to previous version"
    else
        error "Rollback failed - no previous version available"
    fi
    exit 1
fi
info "New container started"

# Wait for application to be healthy
step "Waiting for application health check..."
HEALTH_CHECK_PASSED=false
for i in $(seq 1 $MAX_HEALTH_RETRIES); do
    sleep $HEALTH_RETRY_DELAY
    
    # Check if container is still running
    if ! docker compose ps | grep -q "app.*running"; then
        error "Container stopped unexpectedly"
        docker compose logs --tail=50 app
        break
    fi
    
    # Check health endpoint
    if curl -sf "$HEALTH_ENDPOINT" > /dev/null 2>&1; then
        HEALTH_CHECK_PASSED=true
        info "Health check passed (attempt $i/$MAX_HEALTH_RETRIES)"
        break
    else
        if [[ $i -eq $MAX_HEALTH_RETRIES ]]; then
            error "Health check failed after $MAX_HEALTH_RETRIES attempts"
        else
            echo -n "."
        fi
    fi
done

# If health check failed, rollback
if [[ "$HEALTH_CHECK_PASSED" != "true" ]]; then
    error "Deployment health check failed"
    step "Attempting rollback..."
    
    # Show logs before rollback
    echo ""
    error "Application logs:"
    docker compose logs --tail=100 app
    echo ""
    
    # Rollback
    if docker tag "${IMAGE_NAME}:previous" "$FULL_IMAGE" 2>/dev/null; then
        docker compose up -d --no-deps app
        sleep 5
        
        # Verify rollback health
        if curl -sf "$HEALTH_ENDPOINT" > /dev/null 2>&1; then
            warn "Rolled back to previous version successfully"
            exit 1
        else
            error "Rollback health check also failed - manual intervention required"
            exit 1
        fi
    else
        error "Rollback failed - no previous version available"
        exit 1
    fi
fi

# Ensure nginx is running
step "Ensuring nginx is running..."
docker compose up -d nginx
info "nginx proxy is running"

# Clean up old images to save space
step "Cleaning up old images..."
docker image prune -f --filter "label=org.opencontainers.image.source" || warn "Image cleanup failed"

# Show running containers
step "Deployment summary:"
docker compose ps

# Show application logs (last 20 lines)
echo ""
info "Recent application logs:"
docker compose logs --tail=20 app

# Final health check
echo ""
step "Final health verification..."
HEALTH_RESPONSE=$(curl -s "$HEALTH_ENDPOINT" || echo "{}")
echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"' && \
    info "✓ Application is healthy" || \
    warn "⚠ Health status unclear: $HEALTH_RESPONSE"

# Success
echo ""
info "=================================="
info "✓ Deployment completed successfully"
info "=================================="
info "Image: $FULL_IMAGE"
info "Time: $(date)"
info "Health endpoint: http://45-248-37-116.cloud-xip.com/actuator/health"
echo ""

exit 0
