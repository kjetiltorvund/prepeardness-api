# prepeardness-api

## Kjøre applikasjonen lokalt

Den lokale profilen leser konfigurasjon fra `.env` i prosjektroten. Opprett filen med Supabase REST-verdiene og e-postkonfigurasjonen:

```properties
SUPABASE_URL=https://<prosjekt-id>.supabase.co
SUPABASE_SECRET_API_KEY=<supabase-secret-key>
MAIL_USERNAME=<smtp-brukernavn>
MAIL_PASSWORD=<gmail-app-passord>
```

Applikasjonen bruker Supabase REST API og trenger ikke `DATABASE_URL` eller et PostgreSQL-passord for vanlig lokal kjøring.
E-post sendes med Gmail SMTP. `MAIL_USERNAME` er Gmail-adressen, og `MAIL_PASSWORD` er et Google app-passord.

```shell
mvn spring-boot:run -Dspring.profiles.active=local
```

### Lokal PostgreSQL-integrasjonstest

`PostgreSqlIntegrationSetup` bruker Testcontainers og starter PostgreSQL i Docker automatisk. Docker må kjøre, men testen trenger ingen databasevariabler.

For tester som uttrykkelig kobler til en allerede kjørende lokal PostgreSQL-container, brukes disse variablene bare i testmiljøet:

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/preparednessdb
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<lokalt-passord>
```

Disse databasevariablene skal ikke legges i GitHub-miljøet `production` eller sendes til VPS-en.

## Swagger

The Swagger UI page will then be available at http://server:port/context-path/swagger-ui.html and the OpenAPI description will be available at the following url for json format: http://server:port/context-path/v3/api-docs

server: The server name or IP

port: The server port

context-path: The context path of the application

Documentation can be available in yaml format as well, on the following path : /v3/api-docs.yaml

# Deployment

Produksjonsmiljøet på Kamatera bruker Docker Compose, Nginx, Let's Encrypt og
uforanderlige GHCR-images. Se [deployment-guiden](docs/DEPLOYMENT.md) for første
VPS-oppsett, GitHub-secrets, deployment og rollback.


# SSH Connect

```bash
ssh -o IdentitiesOnly=yes \
  -i ~/.ssh/kamatera_deploy \
  deploy@45-248-37-116.cloud-xip.com
```
