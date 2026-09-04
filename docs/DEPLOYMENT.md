# Produksjonssetting på Kamatera

Applikasjonen kjører som en Spring Boot-container bak Nginx på Ubuntu 24.04. GitHub Actions tester applikasjonen, publiserer et uforanderlig image til GHCR og setter dette imaget i produksjon.

## Produksjonsmiljø

- Vert: `45-248-37-116.cloud-xip.com` (`45.248.37.116`)
- URL: `https://45-248-37-116.cloud-xip.com`
- VPS-katalog: `/opt/prepeardness-api`
- SSH-bruker for deployment: `deploy`
- Database: ekstern Supabase PostgreSQL med TLS
- Offentlig driftssjekk: `GET /actuator/health`

Port 8080 er ikke eksponert fra VPS-en. Swagger og øvrige Actuator-endepunkter returnerer 404 gjennom Nginx.

## 1. Opprett deployment-nøkkel

Kjør lokalt:

```bash
ssh-keygen -t ed25519 -C "github-actions@prepeardness-api" -f ~/.ssh/kamatera_deploy
```

Ta vare på privatnøkkelen. Den offentlige nøkkelen brukes ved bootstrap.

## 2. Bootstrap VPS-en

Logg inn som root via Kamatera-konsollen eller SSH og last ned `scripts/vps-setup.sh`. Les deretter inn innholdet fra den lokale `.pub`-filen før scriptet kjøres. Hvis scriptet kjøres direkte fra den lokale maskinen via SSH:

```bash
DEPLOY_PUBLIC_KEY="$(cat ~/.ssh/kamatera_deploy.pub)"
ssh root@45-248-37-116.cloud-xip.com \
  "CERTBOT_EMAIL='kjetiltorvund@gmail.com' DEPLOY_PUBLIC_KEY='$DEPLOY_PUBLIC_KEY' bash /tmp/vps-setup.sh"
```

Alternativt kan hele innholdet fra `~/.ssh/kamatera_deploy.pub` kopieres manuelt inn som verdien til `DEPLOY_PUBLIC_KEY` i root-terminalen på VPS-en. Ikke bruk privatnøkkelen som denne verdien.

Hvis bootstrap allerede er kjørt uten riktig nøkkel, installer den offentlige nøkkelen fra den lokale maskinen med:

```bash
ssh root@45-248-37-116.cloud-xip.com \
  'install -d -m 700 -o deploy -g deploy /home/deploy/.ssh &&
   touch /home/deploy/.ssh/authorized_keys &&
   chown deploy:deploy /home/deploy/.ssh/authorized_keys &&
   chmod 600 /home/deploy/.ssh/authorized_keys &&
   cat >> /home/deploy/.ssh/authorized_keys' \
  < ~/.ssh/kamatera_deploy.pub
```

Scriptet krever Ubuntu 24.04 og gjør følgende:

- oppdaterer operativsystemet
- installerer Docker Engine, Compose, Certbot, UFW og fail2ban
- oppretter brukeren `deploy` og `/opt/prepeardness-api`
- åpner bare SSH, HTTP og HTTPS
- henter første Let's Encrypt-sertifikat med webroot
- aktiverer automatisk sertifikatfornyelse og Nginx-reload

DNS-navnet må peke til VPS-en, og port 80 må være tilgjengelig før scriptet kjøres.

## 3. Verifiser og sikre SSH

Test deployment-brukeren i en ny terminal før root-tilgang endres:

```bash
ssh -i ~/.ssh/kamatera_deploy deploy@45-248-37-116.cloud-xip.com
docker version
```

Når dette virker, opprett `/etc/ssh/sshd_config.d/99-production-hardening.conf` som root:

```text
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
```

Valider og last SSH på nytt uten å lukke den eksisterende root-økten:

```bash
sshd -t
systemctl reload ssh
```

Test deretter en ny `deploy`-innlogging. Kamatera-konsollen beholdes som gjenopprettingskanal. Medlemskap i Docker-gruppen gir i praksis root-lignende tilgang og skal bare gis til deployment-brukeren.

## 4. Konfigurer GitHub Environment

Opprett miljøet `production` under repository settings. Legg eventuelt på nødvendig godkjenning og beskyttelsesregler.

Følgende environment secrets er påkrevd:

| Navn | Verdi |
| --- | --- |
| `VPS_HOST` | `45-248-37-116.cloud-xip.com` |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | Hele privatnøkkelen fra `~/.ssh/kamatera_deploy` |
| `VPS_KNOWN_HOSTS` | Verifisert known-hosts-linje for VPS-ens SSH-vertsnøkkel |
| `DATABASE_URL` | JDBC-URL med `sslmode=require` |
| `DATABASE_PASSWORD` | Supabase-databasepassord |
| `MAIL_USERNAME` | SMTP-brukernavn |
| `MAIL_PASSWORD` | SMTP-app-passord |
| `SUPABASE_URL` | Supabase-prosjektets HTTPS-URL |
| `SUPABASE_SECRET_API_KEY` | Hemmelig Supabase-nøkkel |
| `SEND_GRID_API_KEY` | SendGrid-nøkkel som applikasjonen forventer |

Hent verts-ID fra en betrodd kanal, for eksempel Kamatera-konsollen:

```bash
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

Sammenlign fingeravtrykket med resultatet fra lokal `ssh-keyscan`, og lagre den verifiserte linjen som `VPS_KNOWN_HOSTS`. Ikke bruk `StrictHostKeyChecking=no`.

GitHubs `GITHUB_TOKEN` opprettes automatisk. Workflowen bruker den til å publisere og hente samme repositories GHCR-image.

## 5. Første deployment

Push til `main`, eller kjør workflowen **Build and deploy to Kamatera** manuelt. Pipeline gjør følgende:

1. kjører `mvn clean verify`
2. bygger og helsetester containeren
3. validerer Compose-konfigurasjonen
4. publiserer `ghcr.io/kjetiltorvund/prepeardness-api:sha-<commit>`
5. overfører Compose-, Nginx- og miljøfilene uten å skrive hemmeligheter til loggen
6. gjenskaper applikasjonscontaineren og venter på Docker-health
7. validerer Nginx og HTTPS-endepunktet
8. kontrollerer at Swagger, øvrig Actuator og port 8080 ikke er offentlige

Produksjonsjobber serialiseres, slik at to releaser ikke endrer VPS-en samtidig.

## Rollback og manuell drift

`deploy.sh` lagrer image-referansen i `.last-good-image` først etter en vellykket deployment. Hvis neste image ikke blir friskt, gjenskapes applikasjonen automatisk med forrige image og workflowen markeres som feilet.

Manuell deployment av et bestemt, allerede publisert image:

```bash
ssh deploy@45-248-37-116.cloud-xip.com
IMAGE_REPOSITORY=ghcr.io/kjetiltorvund/prepeardness-api \
IMAGE_TAG=sha-<full-commit-sha> \
/opt/prepeardness-api/deploy.sh
```

Nyttige kontroller:

```bash
cd /opt/prepeardness-api
docker compose --env-file .env --env-file .deployment.env ps
docker compose --env-file .env --env-file .deployment.env logs --tail=100 app
curl --fail https://45-248-37-116.cloud-xip.com/actuator/health
```

Test sertifikatfornyelse etter første deployment:

```bash
sudo certbot renew --dry-run
systemctl status certbot.timer
```

## Akseptansekontroller

```bash
curl --fail https://45-248-37-116.cloud-xip.com/actuator/health
curl -I http://45-248-37-116.cloud-xip.com/
curl -I https://45-248-37-116.cloud-xip.com/swagger-ui.html
curl -I https://45-248-37-116.cloud-xip.com/actuator/info
```

Forvent henholdsvis HTTP 200 med `UP`, HTTP 301 og HTTP 404 for de to siste kallene.
