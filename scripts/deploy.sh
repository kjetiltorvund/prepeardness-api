#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/prepeardness-api}"
IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-ghcr.io/kjetiltorvund/prepeardness-api}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
FULL_IMAGE="${IMAGE_REPOSITORY}:${IMAGE_TAG}"
STATE_FILE="${DEPLOY_DIR}/.last-good-image"
DEPLOYMENT_ENV="${DEPLOY_DIR}/.deployment.env"
MAX_HEALTH_RETRIES="${MAX_HEALTH_RETRIES:-45}"
HEALTH_RETRY_DELAY="${HEALTH_RETRY_DELAY:-2}"
DOMAIN="${DOMAIN:-45-248-37-116.cloud-xip.com}"

info() { printf '[INFO] %s\n' "$1"; }
error() { printf '[ERROR] %s\n' "$1" >&2; }

write_image_environment() {
    local image_ref="$1"
    local repository="${image_ref%:*}"
    local tag="${image_ref##*:}"
    local temporary_file
    temporary_file="$(mktemp "${DEPLOY_DIR}/.deployment.env.XXXXXX")"
    chmod 600 "$temporary_file"
    printf 'IMAGE_REPOSITORY=%s\nIMAGE_TAG=%s\n' "$repository" "$tag" > "$temporary_file"
    mv "$temporary_file" "$DEPLOYMENT_ENV"
}

compose() {
    docker compose --env-file .env --env-file "$DEPLOYMENT_ENV" "$@"
}

wait_for_health() {
    local container_id status attempt
    container_id="$(compose ps -q app)"
    if [[ -z "$container_id" ]]; then
        error "Application container was not created"
        return 1
    fi

    for attempt in $(seq 1 "$MAX_HEALTH_RETRIES"); do
        status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)"
        case "$status" in
            healthy)
                info "Application is healthy"
                return 0
                ;;
            unhealthy|exited|dead)
                error "Application entered state: $status"
                return 1
                ;;
        esac
        sleep "$HEALTH_RETRY_DELAY"
    done

    error "Application did not become healthy before the timeout"
    return 1
}

deploy_image() {
    local image_ref="$1"
    write_image_environment "$image_ref"
    compose pull app
    compose up -d --no-deps --force-recreate app
    wait_for_health
}

restore_previous_image() {
    if [[ -z "$previous_image" || "$previous_image" == "$FULL_IMAGE" ]]; then
        error "No earlier known-good image is available"
        return 1
    fi

    error "Restoring ${previous_image}"
    deploy_image "$previous_image" || return 1
    compose up -d --no-deps nginx || return 1
    compose exec -T nginx nginx -s reload || return 1
}

verify_proxy() {
    local attempt response
    for attempt in $(seq 1 10); do
        response="$(curl --silent --show-error --fail \
            --noproxy '*' \
            --resolve "${DOMAIN}:443:127.0.0.1" \
            "https://${DOMAIN}/actuator/health" 2>/dev/null || true)"
        if [[ "$response" == *'"status":"UP"'* ]]; then
            return 0
        fi
        sleep 2
    done
    return 1
}

cd "$DEPLOY_DIR"
umask 077

if [[ ! -s .env ]]; then
    error "${DEPLOY_DIR}/.env is missing or empty"
    exit 1
fi

previous_image=""
if [[ -s "$STATE_FILE" ]]; then
    previous_image="$(<"$STATE_FILE")"
fi

info "Deploying immutable image ${FULL_IMAGE}"
write_image_environment "$FULL_IMAGE"
compose config --quiet
compose pull nginx
compose run --rm --no-deps nginx nginx -t

if ! deploy_image "$FULL_IMAGE"; then
    compose logs --tail=100 app || true
    if restore_previous_image; then
        error "Rollback succeeded; the requested deployment remains failed"
    else
        error "Rollback also failed; manual intervention is required"
    fi
    exit 1
fi

if ! compose up -d --no-deps nginx \
    || ! compose exec -T nginx nginx -t \
    || ! compose exec -T nginx nginx -s reload \
    || ! verify_proxy; then
    compose logs --tail=100 nginx || true
    if restore_previous_image && verify_proxy; then
        error "Proxy verification failed; rollback succeeded"
    else
        error "Proxy verification failed and rollback requires manual intervention"
    fi
    exit 1
fi

printf '%s\n' "$FULL_IMAGE" > "$STATE_FILE"
compose ps
docker image prune -f >/dev/null
info "Deployment completed successfully"
