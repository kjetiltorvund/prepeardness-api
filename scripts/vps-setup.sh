#!/bin/bash
set -euo pipefail

# VPS Setup Script for Kamatera Ubuntu 24.04
# This script prepares the VPS for Docker-based deployment
# Run as root: sudo bash vps-setup.sh

echo "=================================="
echo "Kamatera VPS Setup"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   error "This script must be run as root (use sudo)"
fi

info "Starting VPS setup..."

# Update system packages
info "Updating system packages..."
apt-get update
apt-get upgrade -y

# Install prerequisites
info "Installing prerequisites..."
apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    ufw \
    fail2ban

# Install Docker Engine
info "Installing Docker Engine..."
if command -v docker &> /dev/null; then
    warn "Docker is already installed ($(docker --version))"
else
    # Add Docker's official GPG key
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc

    # Add Docker repository
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Start and enable Docker
    systemctl start docker
    systemctl enable docker

    info "Docker installed successfully: $(docker --version)"
fi

# Verify Docker Compose
if docker compose version &> /dev/null; then
    info "Docker Compose is available: $(docker compose version)"
else
    error "Docker Compose plugin not found"
fi

# Configure UFW firewall
info "Configuring UFW firewall..."
ufw --force enable
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'
ufw reload
info "Firewall configured (SSH, HTTP, HTTPS allowed)"

# Configure fail2ban for SSH protection
info "Configuring fail2ban..."
if [[ ! -f /etc/fail2ban/jail.local ]]; then
    cat > /etc/fail2ban/jail.local <<EOF
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
logpath = %(sshd_log)s
EOF
    systemctl enable fail2ban
    systemctl restart fail2ban
    info "fail2ban configured and started"
else
    warn "fail2ban already configured"
fi

# Create deployment directory
info "Creating deployment directory..."
DEPLOY_DIR="/opt/prepeardness-api"
mkdir -p "$DEPLOY_DIR"
cd "$DEPLOY_DIR"
info "Deployment directory created at $DEPLOY_DIR"

# Create .env template
info "Creating .env template..."
if [[ ! -f .env ]]; then
    cat > .env <<EOF
# Application Configuration
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Database Configuration (Supabase)
DATABASE_URL=jdbc:postgresql://db.fhbkafnydplkaqyhqtxm.supabase.co:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=

# Mail Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=kjetiltorvund@gmail.com
MAIL_PASSWORD=

# Supabase Configuration
SUPABASE_URL=
SUPABASE_SECRET_API_KEY=

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_BEREDSKAP=DEBUG
EOF
    chmod 600 .env
    info ".env template created (update with actual secrets)"
else
    warn ".env already exists"
fi

# Create nginx configuration directory
info "Creating nginx configuration..."
mkdir -p nginx/conf.d nginx/ssl
if [[ ! -f nginx/conf.d/app.conf ]]; then
    cat > nginx/conf.d/app.conf <<'EOF'
# nginx configuration for preparedness API

upstream backend {
    server app:8080;
    keepalive 32;
}

server {
    listen 80;
    listen [::]:80;
    server_name 45-248-37-116.cloud-xip.com 45.248.37.116;

    location /actuator/health {
        proxy_pass http://backend;
        access_log off;
    }

    location / {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF
    info "nginx configuration created"
else
    warn "nginx/conf.d/app.conf already exists"
fi

# Create deployment script placeholder
info "Creating deployment script placeholder..."
cat > deploy.sh <<'EOF'
#!/bin/bash
# This script will be updated by the repository's scripts/deploy.sh
echo "Deployment script placeholder - will be overwritten on first deployment"
exit 1
EOF
chmod +x deploy.sh

# Test Docker
info "Testing Docker installation..."
if docker run --rm hello-world &> /dev/null; then
    info "Docker is working correctly"
else
    error "Docker test failed"
fi

# Summary
info ""
info "=================================="
info "VPS Setup Complete!"
info "=================================="
echo ""
echo "✓ Docker Engine installed: $(docker --version)"
echo "✓ Docker Compose available: $(docker compose version)"
echo "✓ Firewall configured (UFW)"
echo "✓ fail2ban enabled"
echo "✓ Deployment directory: $DEPLOY_DIR"
echo ""
echo "Next steps:"
echo "1. Add GitHub Actions SSH public key to /root/.ssh/authorized_keys"
echo "2. Update .env file with actual secrets"
echo "3. Configure GitHub repository secrets"
echo "4. Push code to trigger deployment"
echo ""
warn "Remember to update the .env file with real values before deployment!"
echo ""
