# Deployment Scripts

Dette katalogen inneholder scripts for å sette opp og deploye applikasjonen til Kamatera VPS.

## Scripts

### vps-setup.sh

Initialiserer VPS med alle nødvendige verktøy og konfigurasjon.

**Kjør på VPS (første gang):**
```bash
ssh root@45-248-37-116.cloud-xip.com
curl -o /tmp/vps-setup.sh https://raw.githubusercontent.com/kjetilminde/prepeardness-api/main/scripts/vps-setup.sh
chmod +x /tmp/vps-setup.sh
sudo /tmp/vps-setup.sh
```

**Hva scriptet gjør:**
- Installerer Docker Engine og Docker Compose
- Konfigurerer UFW firewall (porter 22, 80, 443)
- Setter opp fail2ban for SSH-beskyttelse
- Oppretter deployment-katalog `/opt/prepeardness-api/`
- Lager template-filer for `.env` og nginx-konfigurasjon

**Forutsetninger:**
- Ubuntu 24.04
- Root-tilgang

---

### deploy.sh

Kjører deployment av applikasjonen på VPS. Dette scriptet blir automatisk kjørt av GitHub Actions.

**Manuell kjøring (på VPS):**
```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api
./deploy.sh
```

**Hva scriptet gjør:**
1. Autentiserer med GitHub Container Registry
2. Tagger nåværende image som backup
3. Puller nyeste Docker image
4. Deployer med zero-downtime
5. Verifiserer health check
6. Ruller tilbake ved feil

**Miljøvariabler:**
- `IMAGE_NAME` - Docker image-navn (default: ghcr.io/kjetilminde/prepeardness-api)
- `IMAGE_TAG` - Image tag (default: latest)
- `GITHUB_TOKEN` - GitHub token for autentisering
- `GITHUB_ACTOR` - GitHub brukernavnet

**Exit codes:**
- `0` - Deployment vellykket
- `1` - Deployment feilet (rollback utført)

---

## Bruk

### Første gangs oppsett

1. Kjør `vps-setup.sh` på VPS
2. Konfigurer GitHub Secrets
3. Oppdater `.env` på VPS med riktige verdier
4. Push til main branch for å utløse deployment

### Automatisk deployment

GitHub Actions kjører automatisk `deploy.sh` ved push til main branch.

### Manuell deployment

Hvis automatisk deployment ikke fungerer:
```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api
export IMAGE_NAME="ghcr.io/kjetilminde/prepeardness-api"
export IMAGE_TAG="latest"
./deploy.sh
```

---

## Feilsøking

### vps-setup.sh feiler

**Problem:** Docker installasjon feiler
```bash
# Sjekk Ubuntu-versjonen
lsb_release -a

# Manuell Docker-installasjon
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io
```

**Problem:** Firewall blokkerer SSH
```bash
# Deaktiver UFW midlertidig
ufw disable

# Kjør scriptet på nytt
./vps-setup.sh

# UFW aktiveres automatisk av scriptet
```

### deploy.sh feiler

**Problem:** Image pull feiler
```bash
# Logg inn manuelt til ghcr.io
echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_ACTOR --password-stdin

# Prøv å pulle manuelt
docker pull ghcr.io/kjetilminde/prepeardness-api:latest
```

**Problem:** Health check timeout
```bash
# Sjekk container logs
docker compose logs app

# Sjekk om port 8080 er åpen
netstat -tulpn | grep 8080

# Test health endpoint direkte
curl http://localhost:8080/actuator/health
```

**Problem:** Rollback feiler
```bash
# List tilgjengelige images
docker images | grep prepeardness-api

# Manuell rollback til spesifikk versjon
docker tag ghcr.io/kjetilminde/prepeardness-api:main-abc1234 ghcr.io/kjetilminde/prepeardness-api:latest
docker compose up -d
```

---

## Sikkerhet

- Scripts må kjøres som root eller med sudo
- SSH-nøkler brukes for autentisering (ingen passord)
- Miljøvariabler med secrets lagres i `.env` med chmod 600
- Docker images må signeres og verifiseres

---

## Se også

- [Deployment Guide](../docs/DEPLOYMENT.md) - Komplett deployment-dokumentasjon
- [Task Tracking](../.github/tasks/kamatera-deployment.md) - Implementation checklist
