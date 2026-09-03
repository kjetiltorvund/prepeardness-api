# Deployment-script

## `vps-setup.sh`

Kjøres én gang som root på en ren Ubuntu 24.04 VPS. Scriptet installerer driftsmiljøet, oppretter `deploy`-brukeren, konfigurerer nettverksbeskyttelse og henter første TLS-sertifikat.

Påkrevde variabler:

- `CERTBOT_EMAIL`: kontaktadresse for Let's Encrypt
- `DEPLOY_PUBLIC_KEY`: offentlig SSH-nøkkel for GitHub Actions

Valgfrie variabler:

- `DOMAIN`, standard `45-248-37-116.cloud-xip.com`
- `DEPLOY_USER`, standard `deploy`
- `DEPLOY_DIR`, standard `/opt/prepeardness-api`

Eksempel:

```bash
CERTBOT_EMAIL="admin@example.com" \
DEPLOY_PUBLIC_KEY="ssh-ed25519 AAAA... github-actions@prepeardness-api" \
bash vps-setup.sh
```

Scriptet deaktiverer ikke root-innlogging automatisk. Følg den verifiserte rekkefølgen i [deployment-guiden](../docs/DEPLOYMENT.md) for å unngå å låse serveren.

## `deploy.sh`

Kjøres som `deploy` av GitHub Actions. Scriptet krever en ferdig `/opt/prepeardness-api/.env` og følgende variabler:

- `IMAGE_REPOSITORY`
- `IMAGE_TAG`, alltid den uforanderlige `sha-<commit>`-taggen

Scriptet validerer Compose og Nginx, henter imaget, gjenskaper app-containeren og venter på Docker-health. Etter suksess oppdateres `.last-good-image`. Ved feil forsøkes automatisk rollback til verdien som allerede står i denne filen.

Eksempel:

```bash
IMAGE_REPOSITORY=ghcr.io/kjetiltorvund/prepeardness-api \
IMAGE_TAG=sha-<full-commit-sha> \
/opt/prepeardness-api/deploy.sh
```

Se [deployment-guiden](../docs/DEPLOYMENT.md) for bootstrap, GitHub-secrets, SSH-herding og feilsøking.
