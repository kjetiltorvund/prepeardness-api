# Kamatera deployment status

Repository implementation is complete for the following production design:

- Ubuntu 24.04 bootstrap with Docker, UFW, fail2ban and Certbot
- dedicated `deploy` account and key-based SSH
- one GitHub Actions workflow for test, image publication and deployment
- immutable `sha-<commit>` GHCR images
- Docker-health verification and last-known-good rollback
- Nginx HTTPS proxy with automatic certificate renewal
- public health endpoint only; Swagger and other Actuator endpoints denied

Remaining operator actions:

- [ ] Generate the GitHub Actions deployment key.
- [ ] Run `scripts/vps-setup.sh` as root with `CERTBOT_EMAIL` and `DEPLOY_PUBLIC_KEY`.
- [ ] Verify SSH and Docker access as `deploy`.
- [ ] Disable password and root SSH login using `docs/DEPLOYMENT.md`.
- [ ] Create and protect the GitHub `production` environment.
- [ ] Add every secret listed in `docs/DEPLOYMENT.md`.
- [ ] Run the workflow and verify the acceptance checks.
- [ ] Run `certbot renew --dry-run` on the VPS.
- [ ] Exercise rollback once with an intentionally unhealthy test image.
