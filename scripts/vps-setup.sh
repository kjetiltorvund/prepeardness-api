#!/usr/bin/env bash
set -euo pipefail

DEPLOY_USER="${DEPLOY_USER:-deploy}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/prepeardness-api}"
DOMAIN="${DOMAIN:-45-248-37-116.cloud-xip.com}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:?Set CERTBOT_EMAIL before running this script}"
DEPLOY_PUBLIC_KEY="${DEPLOY_PUBLIC_KEY:?Set DEPLOY_PUBLIC_KEY to the complete contents of the generated .pub file}"

info() { printf '[INFO] %s\n' "$1"; }

if [[ "$EUID" -ne 0 ]]; then
    printf '[ERROR] Run this script as root\n' >&2
    exit 1
fi

if [[ ! -r /etc/os-release ]]; then
    printf '[ERROR] Cannot identify the operating system\n' >&2
    exit 1
fi

. /etc/os-release
if [[ "${ID:-}" != "ubuntu" || "${VERSION_ID:-}" != "24.04" ]]; then
    printf '[ERROR] This script supports Ubuntu 24.04 only\n' >&2
    exit 1
fi

info "Updating Ubuntu and installing base packages"
apt update
DEBIAN_FRONTEND=noninteractive apt upgrade -y
DEBIAN_FRONTEND=noninteractive apt install -y ca-certificates curl gnupg ufw fail2ban certbot

if ! command -v docker >/dev/null 2>&1; then
    info "Installing Docker Engine"
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    printf 'deb [arch=%s signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu %s stable\n' \
        "$(dpkg --print-architecture)" "$VERSION_CODENAME" \
        > /etc/apt/sources.list.d/docker.list
    apt update
    DEBIAN_FRONTEND=noninteractive apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
systemctl enable --now docker fail2ban
docker compose version >/dev/null

info "Creating the deployment account"
if ! id "$DEPLOY_USER" >/dev/null 2>&1; then
    useradd --create-home --shell /bin/bash "$DEPLOY_USER"
fi
usermod -aG docker "$DEPLOY_USER"
install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "/home/${DEPLOY_USER}/.ssh"
printf '%s\n' "$DEPLOY_PUBLIC_KEY" > "/home/${DEPLOY_USER}/.ssh/authorized_keys"
chown "$DEPLOY_USER:$DEPLOY_USER" "/home/${DEPLOY_USER}/.ssh/authorized_keys"
chmod 600 "/home/${DEPLOY_USER}/.ssh/authorized_keys"
install -d -m 750 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$DEPLOY_DIR" "$DEPLOY_DIR/nginx" "$DEPLOY_DIR/nginx/conf.d"
install -d -m 755 /var/www/certbot

info "Configuring the firewall"
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

info "Configuring fail2ban"
install -d -m 755 /etc/fail2ban/jail.d
cat > /etc/fail2ban/jail.d/sshd.local <<'EOF'
[sshd]
enabled = true
bantime = 1h
findtime = 10m
maxretry = 5
EOF
systemctl restart fail2ban

if [[ ! -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]]; then
    info "Obtaining the initial Let's Encrypt certificate"
    bootstrap_dir="$(mktemp -d)"
    bootstrap_container="certbot-bootstrap"
    cleanup() {
        docker rm -f "$bootstrap_container" >/dev/null 2>&1 || true
        rm -rf "$bootstrap_dir"
    }
    trap cleanup EXIT

    cat > "${bootstrap_dir}/default.conf" <<EOF
server {
    listen 80;
    server_name ${DOMAIN};
    location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 404; }
}
EOF
    docker run -d --name "$bootstrap_container" -p 80:80 \
        -v "${bootstrap_dir}/default.conf:/etc/nginx/conf.d/default.conf:ro" \
        -v /var/www/certbot:/var/www/certbot:ro nginx:1.27-alpine >/dev/null
    certbot certonly --webroot --webroot-path /var/www/certbot \
        --domain "$DOMAIN" --email "$CERTBOT_EMAIL" --agree-tos --no-eff-email --non-interactive
    cleanup
    trap - EXIT
fi

info "Configuring automatic certificate renewal"
install -d -m 755 /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/reload-prepeardness-nginx <<EOF
#!/usr/bin/env bash
set -euo pipefail
cd ${DEPLOY_DIR}
if [[ -s .env && -s .deployment.env ]] && \
    docker compose --env-file .env --env-file .deployment.env ps --status running nginx | grep -q nginx; then
    docker compose --env-file .env --env-file .deployment.env exec -T nginx nginx -t
    docker compose --env-file .env --env-file .deployment.env exec -T nginx nginx -s reload
fi
EOF
chmod 755 /etc/letsencrypt/renewal-hooks/deploy/reload-prepeardness-nginx
systemctl enable --now certbot.timer

info "VPS bootstrap completed"
printf 'Verify access with: ssh %s@%s\n' "$DEPLOY_USER" "$DOMAIN"
printf 'After verification, disable password and root SSH login as documented in docs/DEPLOYMENT.md.\n'
