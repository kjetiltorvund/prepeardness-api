# prepeardness-api

## Running the application

Locally we use a .env file to hold our input values to the application. To actively use it we have to specify which
profile to use. For this we use `local`.
This tells the Spring Boot framework to use the file `application-local-properties`. This in turn loads values from the
`.env` file.

```shell
mvn spring-boot:run -Dspring.profiles.active=local 
```

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