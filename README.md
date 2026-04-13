# prepeardness-api

## Swagger

The Swagger UI page will then be available at http://server:port/context-path/swagger-ui.html and the OpenAPI description will be available at the following url for json format: http://server:port/context-path/v3/api-docs

server: The server name or IP

port: The server port

context-path: The context path of the application

Documentation can be available in yaml format as well, on the following path : /v3/api-docs.yaml

# HTTPS with Let's Encrypt

docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d 45-248-37-116.cloud-xip.com \
  --email kjetiltorvund@gmail.com --agree-tos --no-eff-email