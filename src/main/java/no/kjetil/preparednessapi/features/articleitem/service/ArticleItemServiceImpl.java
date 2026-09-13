package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateArticleItemDto;

import no.kjetil.preparednessapi.features.articleitem.dtos.UpdateArticleDto;
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
import java.util.stream.Collectors;

@Service
public class ArticleItemServiceImpl implements ArticleItemService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleItemServiceImpl.class);
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String apiKeyParameter = "apiKey";
    private final String basePath = "/rest/v1/article_items";

    public ArticleItemServiceImpl(
            ArticleItemServiceProperties options,
            HttpClient httpClient,
            ModelMapper modelMapper,
            ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.apiBaseUrl = options.getUrl();
        this.apiKey = options.getApiKey();
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ArticleItem createArticleItem(ArticleItem articleItem) {

        CreateArticleItemDto createArticleItemDto = modelMapper.map(articleItem, CreateArticleItemDto.class);

        HttpRequest request = createPostRequest(basePath, createArticleItemDto);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() < HttpStatus.OK.value()) {
                String message = String.format("Unable to create object. Return code: %d", response.statusCode());
                throw new RuntimeException(message);
            }

            List<ArticleItemDto> result = objectMapper.readValue(response.body(), new TypeReference<List<ArticleItemDto>>() {
            });

            return result.stream().map(item -> modelMapper.map(item, ArticleItem.class)).collect(Collectors.toList()).getFirst();
        } catch (IOException | InterruptedException e) {
            logger.error("Error creating ArticleItem", e);
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unused")
    private List<ArticleItem> findByArticleName(String articleName) {
        String uriPath = basePath + "?article_name=eq." + articleName;

        HttpRequest request = createGetRequest(uriPath);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            List<ArticleItemDto> articleItems = objectMapper.readValue(body, new TypeReference<List<ArticleItemDto>>() {
            });
            if (articleItems.isEmpty()) {
                return null;
            }
            return articleItems.stream().map(article -> modelMapper.map(article, ArticleItem.class)).collect(Collectors.toList());
        } catch (IOException | InterruptedException e) {
            logger.error("Error finding ArticleItem by name", e);
            e.printStackTrace();
        }
        return null;
    }

    private HttpRequest createPostRequest(String uriPath, CreateArticleItemDto articleItem) {
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
        String uriPath = basePath + "?id=eq." + id;
        HttpRequest request = createGetRequest(uriPath);

        logger.info("Sending request to URL: {}", request.uri().toString());

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

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
                .headers(apiKeyParameter, apiKey, "Prefer", "return=representation")
        .build();
    }

    private HttpRequest createPutRequest(String uriPath, String body) {
        String uri = apiBaseUrl + uriPath;

        return HttpRequest.newBuilder()
        .PUT(HttpRequest.BodyPublishers.ofString(body))
        .uri(URI.create(uri))
                .headers(apiKeyParameter, apiKey, "Prefer", "return=representation")
        .build();
    }

    @Override
    public ArticleItem updateArticleItem(ArticleItem articleItem) {
        String uriPath = basePath + "?id=eq." + articleItem.getId();

        UpdateArticleDto updateArticleDto = modelMapper.map(articleItem, UpdateArticleDto.class);

        String body = convertBodyToString(updateArticleDto);

        HttpRequest request = createPutRequest(uriPath, body);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value() || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                String message = String.format("Unable to update object. Return code: %d, body: %s", response.statusCode(), response.body());
                throw new RuntimeException(message);
            }

            List<ArticleItemDto> updatedItems = objectMapper.readValue(response.body(), new TypeReference<List<ArticleItemDto>>() {
            });

            return updatedItems.stream()
                    .findFirst()
                    .map(item -> modelMapper.map(item, ArticleItem.class))
                    .orElseThrow(() -> new RuntimeException("Update response did not contain any article items"));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertBodyToString(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
        String uriPath = basePath + "?id=eq." + id;
        HttpRequest request = createGetRequest(uriPath);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new RuntimeException("Unable to find ArticleItem. Return code: " + response.statusCode());
            }

            List<ArticleItemDto> articleItems = objectMapper.readValue(
                    response.body(), new TypeReference<List<ArticleItemDto>>() {});

            return articleItems.stream()
                    .findFirst()
                    .map(item -> modelMapper.map(item, ArticleItem.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
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
        }

        return statusCode;
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
