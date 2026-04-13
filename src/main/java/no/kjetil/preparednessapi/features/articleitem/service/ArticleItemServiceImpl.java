package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.management.RuntimeMBeanException;

@Service
public class ArticleItemServiceImpl implements ArticleItemService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleItemServiceImpl.class);
    private final ModelMapper modelMapper;

    private HttpClient httpClient;
    private String apiBaseUrl;
    private String apiKey;
    private String apiKeyParameter = "apiKey";

    public ArticleItemServiceImpl(ArticleItemServiceProperties options, 
        HttpClient httpClient,
                                  ModelMapper modelMapper) {
        this.httpClient = httpClient;
        this.apiBaseUrl = options.getUrl();
        this.apiKey = options.getApiKey();
        this.modelMapper = modelMapper;
    }

    @Override
    public ArticleItem createArticleItem(ArticleItem articleItem) {
        HttpRequest request = createPostRequest(null, articleItem);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() < HttpStatus.OK.value()) {
                String message = String.format("Unable to create object. Return code: {}", response.statusCode());
                throw new RuntimeException(message);
            }

            String body = response.body();

            ArticleItem createdItem = modelMapper.map(body, ArticleItem.class);

            return createdItem;
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            logger.error("Error creating ArticleItem", e);
            e.printStackTrace();
        }
        return null;
    }

    private HttpRequest createPostRequest(String uriPath, ArticleItem articleItem) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String bodyAsJson = objectMapper.writeValueAsString(articleItem);

            return createPostRequest(uriPath, bodyAsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ArticleItem readArticleItemById(long id) {
        String uriPath = "/rest/v1/article_items?id=eq." + id;
        HttpRequest request = createGetRequest(uriPath);

        logger.info("Sending request to URL: {}", request.uri().toString());

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            ObjectMapper objectMapper = new ObjectMapper();

            // Assuming the response is a JSON array and we want the first item
            body = body.substring(1, body.length() - 1); // Remove the surrounding
            ArticleItemDto dto = objectMapper.readValue(body, ArticleItemDto.class);
            logger.info("Received ArticleItemDto: {}", dto);
            return modelMapper.map(dto, ArticleItem.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest createGetRequest(String uriPath) {
        String uri = apiBaseUrl + uriPath;
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(30))
                .headers(
                        apiKeyParameter, apiKey
                ).build();
        return request;
    }

    private HttpRequest createPostRequest(String uriPath, String body) {
        String uri = apiBaseUrl + (StringUtils.isBlank(body) ? "" : uriPath);
        
        return HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .uri(URI.create(uri))
        .headers(apiKeyParameter, apiKey)
        .build();
    }

    private HttpRequest createPutRequest(String uriPath, String body) {
        String uri = apiBaseUrl + uriPath;

        return HttpRequest.newBuilder()
        .PUT(HttpRequest.BodyPublishers.ofString(body))
        .uri(URI.create(uri))
        .headers(apiKeyParameter, apiKey)
        .build();
    }

    @Override
    public ArticleItem updateArticleItem(ArticleItem articleItem) {
        return null;
    }

    @Override
    public void deleteArticleItemById(long id) {

    }

    @Override
    public List<ArticleItem> findAllByDatePassedExpirationDate(Date date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllByDatePassedExpirationDate'");
    }

    @Override
    public Optional<ArticleItem> findById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public List<ArticleItem> findAll() {
        String uriPath = "/rest/v1/article_items";

        HttpRequest request = createGetRequest(uriPath);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            ObjectMapper objectMapper = new ObjectMapper();

            List<ArticleItemDto> articleItemDtos = objectMapper.readValue(body, new TypeReference<List<ArticleItemDto>>() {});

            return modelMapper.map(articleItemDtos, new TypeToken<List<ArticleItem>>() {}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public HttpStatusCode save(ArticleItem articleItem) {
        String uriPath = "/rest/v1/article_items";

        
        String body = getBody(articleItem).orElse("");

        HttpRequest request = createPostRequest(uriPath, body);
        
        HttpStatusCode statusCode = HttpStatus.UNPROCESSABLE_ENTITY;

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            statusCode = HttpStatusCode.valueOf(response.statusCode());

            if(response.statusCode() >= 200 && response.statusCode() < 300) {
                String responseBody = response.body().toString();
                ObjectMapper objectMapper = new ObjectMapper();
                ArticleItemDto dto = objectMapper.readValue(responseBody, ArticleItemDto.class);
                return HttpStatusCode.valueOf(response.statusCode());
            } else {
                logger.error("Failed to save ArticleItem. Status code: {}, Response body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to save ArticleItem. Status code: " + response.statusCode());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            return statusCode;
        }
    }

    private Optional<String> getBody(ArticleItem articleItem) {
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            return Optional.of(objectMapper.writeValueAsString(articleItem));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    

    @Override
    public List<ArticleItem> saveAll(List<ArticleItem> articleItems) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveAll'");
    }

    @Override
    public void deleteById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }
}
