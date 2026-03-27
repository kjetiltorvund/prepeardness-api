# Kamatera VPS Deployment Guide

Dette dokumentet beskriver hvordan man setter opp og deployer Preparedness API til en Kamatera VPS.

## Oversikt

- **VPS:** 45-248-37-116.cloud-xip.com (45.248.37.116)
- **OS:** Ubuntu 24.04
- **Applikasjon:** Spring Boot 3.5.8 med Java 21
- **Database:** Ekstern Supabase PostgreSQL
- **Registry:** GitHub Container Registry (ghcr.io)
- **Deployment:** Automatisk ved push til main branch

## Forutsetninger

- Root-tilgang til VPS
- GitHub repository med riktige tilganger
- Supabase database konfigurert
- Gmail-konto for e-post (eller annen SMTP)

## Oppsett (Første gang)

### 1. Kjør VPS-oppsettscriptet

Logg inn på VPS og kjør setup-scriptet:

```bash
ssh root@45-248-37-116.cloud-xip.com
cd /tmp
curl -o vps-setup.sh https://raw.githubusercontent.com/kjetilminde/prepeardness-api/main/scripts/vps-setup.sh
chmod +x vps-setup.sh
sudo ./vps-setup.sh
```

Dette installerer:
- Docker Engine og Docker Compose
- UFW firewall (porter 22, 80, 443)
- fail2ban for SSH-beskyttelse
- Deployment-katalog i `/opt/prepeardness-api/`

### 2. Generer SSH-nøkler for GitHub Actions

På din lokale maskin:

```bash
ssh-keygen -t ed25519 -C "github-actions@prepeardness-api" -f ~/.ssh/kamatera_deploy
```

Legg til den offentlige nøkkelen på VPS:

```bash
ssh root@45-248-37-116.cloud-xip.com "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys" < ~/.ssh/kamatera_deploy.pub
```

### 3. Konfigurer GitHub Secrets

Naviger til repository settings → Secrets and variables → Actions, og legg til:

| Secret Name | Description | Example |
|------------|-------------|---------|
| `VPS_SSH_KEY` | Private SSH-nøkkel | Innhold av `~/.ssh/kamatera_deploy` |
| `VPS_HOST` | VPS hostname | `45-248-37-116.cloud-xip.com` |
| `VPS_USER` | SSH-bruker | `root` |
| `DATABASE_URL` | PostgreSQL connection string | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require` |
| `DATABASE_PASSWORD` | Database passord | `***` |
| `MAIL_USERNAME` | Gmail-adresse | `kjetiltorvund@gmail.com` |
| `MAIL_PASSWORD` | Gmail app password | `***` |
| `SUPABASE_URL` | Supabase project URL | `https://xxx.supabase.co` |
| `SUPABASE_SECRET_API_KEY` | Supabase secret key | `***` |

**Merk:** `GITHUB_TOKEN` er automatisk tilgjengelig og brukes for å autentisere med ghcr.io.

### 4. Oppdater .env på VPS

SSH til VPS og rediger `.env`-filen:

```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api
nano .env
```

Fyll inn alle verdier (samme som GitHub Secrets).

### 5. Deploy første versjon

Push kode til main branch eller kjør workflow manuelt:

```bash
git push origin main
```

Eller i GitHub: Actions → Deploy to Kamatera VPS → Run workflow

## Deployment-prosess

### Automatisk deployment

Når du pusher til `main` branch:

1. **Build and Test:**
   - Kompilerer med Maven
   - Kjører tester
   - Laster opp JAR som artifact

2. **Build and Push Image:**
   - Bygger Docker image
   - Pusher til ghcr.io med tags `latest` og `main-SHA`
   - Cacher layers for raskere builds

3. **Deploy:**
   - Kopierer `docker-compose.yaml` og `deploy.sh` til VPS
   - Oppdaterer `.env` med secrets
   - Kjører deployment-script
   - Verifiserer health endpoint

4. **Verify:**
   - Sjekker at applikasjonen svarer
   - Bekrefter health status

### Manuell deployment

Hvis du trenger å deploye manuelt:

```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api
./deploy.sh
```

## Zero-downtime deployment

Deployment-scriptet sikrer zero-downtime ved å:

1. Beholde gammel container kjørende
2. Starte ny container
3. Vente på health check
4. Stoppe gammel container ved suksess
5. Rulle tilbake ved feil

## Rollback

### Automatisk rollback

Hvis health check feiler, ruller deployment-scriptet automatisk tilbake til forrige versjon.

### Manuell rollback

```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api

# Stopp nåværende versjon
docker compose down

# Tag forrige versjon som latest
docker tag ghcr.io/kjetilminde/prepeardness-api:previous ghcr.io/kjetilminde/prepeardness-api:latest

# Start forrige versjon
docker compose up -d
```

## SSL/HTTPS Oppsett

For å aktivere HTTPS med Let's Encrypt:

```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api

# Installer certbot
apt-get install -y certbot

# Generer sertifikat
certbot certonly --standalone -d 45-248-37-116.cloud-xip.com \
  --email kjetiltorvund@gmail.com --agree-tos --no-eff-email

# Kopier sertifikater til nginx-mappen
mkdir -p nginx/ssl
cp /etc/letsencrypt/live/45-248-37-116.cloud-xip.com/fullchain.pem nginx/ssl/cert.pem
cp /etc/letsencrypt/live/45-248-37-116.cloud-xip.com/privkey.pem nginx/ssl/key.pem

# Oppdater nginx-konfigurasjon (fjern kommentarer fra HTTPS-server)
nano nginx/conf.d/app.conf

# Restart nginx
docker compose restart nginx
```

## Overvåking og logging

### Se applikasjonslogger

```bash
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api

# Live logs
docker compose logs -f app

# Siste 100 linjer
docker compose logs --tail=100 app
```

### Health check

```bash
curl http://45-248-37-116.cloud-xip.com/actuator/health
```

Forventet respons:
```json
{
  "status": "UP"
}
```

### Container status

```bash
docker compose ps
```

## Feilsøking

### Applikasjonen starter ikke

```bash
# Sjekk logger
docker compose logs app

# Sjekk miljøvariabler
docker compose exec app env | grep -i spring

# Test database-tilkobling
docker compose exec app curl -f localhost:8080/actuator/health
```

### Deployment feiler

```bash
# Sjekk GitHub Actions logs
# Actions → Deploy to Kamatera VPS → [latest run]

# Sjekk VPS deployment logs
ssh root@45-248-37-116.cloud-xip.com
cd /opt/prepeardness-api
docker compose logs --tail=100
```

### Kan ikke nå applikasjonen

```bash
# Sjekk firewall
sudo ufw status

# Sjekk nginx
docker compose logs nginx

# Test direkte mot app (fra VPS)
curl http://localhost:8080/actuator/health
```

## Vedlikehold

### Oppdater Docker images

```bash
ssh root@45-248-37-116.cloud-xip.com
docker system prune -a -f
```

### Backup

Database er hostet eksternt (Supabase), så ingen backup nødvendig på VPS.

For å ta backup av konfigurasjon:

```bash
ssh root@45-248-37-116.cloud-xip.com
tar -czf /tmp/prepeardness-config-$(date +%Y%m%d).tar.gz \
  /opt/prepeardness-api/.env \
  /opt/prepeardness-api/nginx/conf.d/
```

### Oppdater VPS-programvare

```bash
ssh root@45-248-37-116.cloud-xip.com
apt-get update && apt-get upgrade -y
# Restart om nødvendig
```

## Sikkerhet

- SSH nøkkelbasert autentisering (passord deaktivert)
- fail2ban aktivert for SSH-beskyttelse
- UFW firewall med kun nødvendige porter åpne
- Database kjører eksternt (ikke eksponert)
- Miljøvariabler lagres sikkert i GitHub Secrets
- Docker images signert og verifisert

## Kontakt

For problemer eller spørsmål, kontakt team-lead eller opprett en issue i GitHub repository.
