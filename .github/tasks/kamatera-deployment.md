# Kamatera VPS Deployment - Implementation Tasks

## VPS Information
- **Host:** 45-248-37-116.cloud-xip.com (45.248.37.116)
- **OS:** Ubuntu 24.04
- **User:** root
- **Application:** prepeardness-api (Spring Boot 3.5.8, Java 21)
- **Database:** External Supabase PostgreSQL
- **Registry:** GitHub Container Registry (ghcr.io)

## Implementation Checklist

### Phase 1: VPS Setup
- [ ] 1. Run VPS setup script (`scripts/vps-setup.sh`) on server
  - [ ] Install Docker Engine
  - [ ] Install Docker Compose
  - [ ] Configure UFW firewall (ports 22, 80, 443)
  - [ ] Set up SSH key-based authentication
  - [ ] Create deployment directory `/opt/prepeardness-api/`
  - [ ] Install necessary tools (git, curl)

### Phase 2: GitHub Configuration
- [ ] 2. Add GitHub Secrets to repository
  - [ ] VPS_SSH_KEY (private SSH key for deployment)
  - [ ] VPS_HOST (45-248-37-116.cloud-xip.com)
  - [ ] VPS_USER (root)
  - [ ] DATABASE_PASSWORD
  - [ ] MAIL_PASSWORD
  - [ ] SUPABASE_URL
  - [ ] SUPABASE_SECRET_API_KEY
  - [ ] Verify GITHUB_TOKEN (automatic)

### Phase 3: Repository Updates
- [ ] 3. Update docker-compose.yaml with ghcr.io image path
- [ ] 4. Verify nginx configuration for reverse proxy
- [ ] 5. Commit and push deployment scripts and workflow

### Phase 4: Initial Deployment
- [ ] 6. Generate and configure SSH key pair for GitHub Actions
- [ ] 7. Test GitHub Actions workflow on feature branch
- [ ] 8. Deploy initial version to VPS
- [ ] 9. Configure nginx on VPS
- [ ] 10. Set up SSL certificate (Let's Encrypt)

### Phase 5: Verification
- [ ] 11. Verify application health endpoint
- [ ] 12. Test full deployment pipeline (push to main)
- [ ] 13. Verify zero-downtime deployment
- [ ] 14. Test rollback procedure

## Notes

### SSH Key Setup
```bash
# On local machine
ssh-keygen -t ed25519 -C "github-actions@prepeardness-api" -f ~/.ssh/kamatera_deploy
# Add public key to VPS: ~/.ssh/authorized_keys
# Add private key to GitHub Secrets as VPS_SSH_KEY
```

### Manual Deployment (if needed)
```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api
./deploy.sh
```

### Health Check Endpoint
- URL: http://45-248-37-116.cloud-xip.com/actuator/health
- Expected: HTTP 200 with `{"status":"UP"}`

### Rollback Procedure
If deployment fails, the deployment script automatically reverts to the previous version.

Manual rollback:
```bash
docker-compose down
docker tag ghcr.io/kjetilminde/prepeardness-api:previous ghcr.io/kjetilminde/prepeardness-api:latest
docker-compose up -d
```

## Completion Criteria
- [ ] Application accessible via HTTP/HTTPS
- [ ] Automated deployment on push to main
- [ ] Health checks passing
- [ ] Zero-downtime deployment verified
- [ ] Rollback procedure tested
