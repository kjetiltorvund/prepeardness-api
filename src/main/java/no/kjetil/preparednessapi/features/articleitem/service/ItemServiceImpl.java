package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateItemResponse;
import no.kjetil.preparednessapi.features.articleitem.dtos.ItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.UpdateArticleDto;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemServiceImpl.class);
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String apiKeyParameter = "apiKey";
    private final String basePath = "/rest/v1/article_items";

    public ItemServiceImpl(
            ItemServiceProperties options,
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
    @CacheEvict(value = "articles", allEntries = true)
    public ArticleItem createArticleItem(ArticleItem articleItem) {

        CreateItemDto createItemDto = modelMapper.map(articleItem, CreateItemDto.class);

        HttpRequest request = createPostRequest(createItemDto);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()) {
                String message = String.format("Unable to create object. Return code: %d", response.statusCode());
                throw new RuntimeException(message);
            }

            List<ItemDto> result = objectMapper.readValue(response.body(),
                    new TypeReference<>() {
                    });

            return result.stream().map(item -> modelMapper.map(item, ArticleItem.class)).toList()
                    .getFirst();
        } catch (IOException e) {
            logRequestFailure("Create ArticleItem", request, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Create ArticleItem", request, e);
        }
        return null;
    }

    private void logRequestFailure(String operation, HttpRequest request, Exception e) {
        logger.error("{} failed for {} {}: {} - {}", operation, request.method(), request.uri(),
                e.getClass().getSimpleName(), e.getMessage(), e);
    }

    private void logRequestInterrupted(String operation, HttpRequest request, InterruptedException e) {
        logger.warn("{} was interrupted for {} {}", operation, request.method(), request.uri(), e);
    }

    @SuppressWarnings("unused")
    private List<ArticleItem> findByArticleName(String articleName) {
        String uriPath = basePath + "?article_name=eq." + articleName;

        HttpRequest request = createGetRequest(uriPath);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            List<ItemDto> articleItems = objectMapper.readValue(body, new TypeReference<>() {
            });
            if (articleItems.isEmpty()) {
                return null;
            }
            return articleItems.stream().map(article -> modelMapper.map(article, ArticleItem.class))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logRequestFailure("Find ArticleItem by name '" + articleName + "'", request, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Find ArticleItem by name '" + articleName + "'", request, e);
        }
        return null;
    }

    private HttpRequest createPostRequest(CreateItemDto articleItem) {
        try {
            String bodyAsJson = objectMapper.writeValueAsString(articleItem);

            return createPostRequest("/rest/v1/article_items", bodyAsJson);
        } catch (JsonProcessingException e) {
            logger.error("Unable to serialize CreateArticleItemDto for POST {}: {}", "/rest/v1/article_items", e.getOriginalMessage(), e);
            throw new RuntimeException("Unable to serialize CreateArticleItemDto", e);
        }
    }

    @Override
    @Cacheable(value = "articles", key = "#id")
    public ArticleItem readArticleItemById(long id) {
        String uriPath = basePath + "?id=eq." + id;
        HttpRequest request = createGetRequest(uriPath);

        logger.info("Sending request to URL: {}", request.uri().toString());

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            // Assuming the response is a JSON array and we want the first item
            body = body.substring(1, body.length() - 1); // Remove the surrounding
            ItemDto dto = objectMapper.readValue(body, ItemDto.class);
            logger.info("Received ArticleItemDto: {}", dto);
            return modelMapper.map(dto, ArticleItem.class);
        } catch (IOException e) {
            logRequestFailure("Read ArticleItem with id " + id, request, e);
            throw new RuntimeException("Unable to read ArticleItem with id " + id, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Read ArticleItem with id " + id, request, e);
            throw new RuntimeException("Interrupted while reading ArticleItem with id " + id, e);
        }
    }

    private HttpRequest createGetRequest(String uriPath) {
        String url = apiBaseUrl + uriPath;
        URI uri = URI.create(url);
        return HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .headers(
                        apiKeyParameter, apiKey)
                .build();
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

    private HttpRequest createPatchRequest(String uriPath, String body) {
        String uri = apiBaseUrl + uriPath;

        return HttpRequest.newBuilder()
                .method(HttpMethod.PATCH.name(), HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(uri))
                .headers(apiKeyParameter, apiKey, "Prefer", "return=representation")
                .build();
    }

    public ArticleItem patchUpdateArticleItem(ArticleItem articleItem) {
        String uriPath = basePath + "?id=eq." + articleItem.getId();

        UpdateArticleDto updateArticleDto = modelMapper.map(articleItem, UpdateArticleDto.class);

        String body = convertBodyToString(updateArticleDto);

        HttpRequest request = createPutRequest(uriPath, body);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                String message = String.format("Unable to update object. Return code: %d, body: %s",
                        response.statusCode(), response.body());
                throw new RuntimeException(message);
            }

            List<ItemDto> updatedItems = objectMapper.readValue(response.body(),
                    new TypeReference<>() {
                    });

            return updatedItems.stream()
                    .findFirst()
                    .map(item -> modelMapper.map(item, ArticleItem.class))
                    .orElseThrow(() -> new RuntimeException("Update response did not contain any article items"));
        } catch (IOException e) {
            logRequestFailure("Update ArticleItem with id " + articleItem.getId(), request, e);
            throw new RuntimeException("Unable to update ArticleItem with id " + articleItem.getId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Update ArticleItem with id " + articleItem.getId(), request, e);
            throw new RuntimeException("Interrupted while updating ArticleItem with id " + articleItem.getId(), e);
        }
    }

    @Override
    @CacheEvict(value = "articles", allEntries = true)
    public ArticleItem updateArticleItem(ArticleItem articleItem) {
        String uriPath = basePath + "?id=eq." + articleItem.getId();

        UpdateArticleDto updateArticleDto = modelMapper.map(articleItem, UpdateArticleDto.class);

        String body = convertBodyToString(updateArticleDto);

        HttpRequest request = createPatchRequest(uriPath, body);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                String message = String.format("Unable to update object. Return code: %d, body: %s",
                        response.statusCode(), response.body());
                throw new RuntimeException(message);
            }

            List<ItemDto> updatedItems = objectMapper.readValue(response.body(),
                    new TypeReference<>() {
                    });

            return updatedItems.stream()
                    .findFirst()
                    .map(item -> modelMapper.map(item, ArticleItem.class))
                    .orElseThrow(() -> new RuntimeException("Update response did not contain any article items"));
        } catch (IOException e) {
            logRequestFailure("Update ArticleItem with id " + articleItem.getId(), request, e);
            throw new RuntimeException("Unable to update ArticleItem with id " + articleItem.getId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Update ArticleItem with id " + articleItem.getId(), request, e);
            throw new RuntimeException("Interrupted while updating ArticleItem with id " + articleItem.getId(), e);
        }
    }

    private String convertBodyToString(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Unable to serialize {}: {}", payload.getClass().getSimpleName(), e.getOriginalMessage(), e);
            throw new RuntimeException("Unable to serialize " + payload.getClass().getSimpleName(), e);
        }
    }

    @Override
    @CacheEvict(value = "articles", allEntries = true)
    public List<ItemDto> deleteArticleItemById(long id) {
        String uriPath = basePath + "?id=eq." + id;

        HttpRequest request = createDeleteRequest(uriPath);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new RuntimeException("Unable to delete ArticleItem. Return code: " + response.statusCode());
            }

            if ("[]".equals(response.body().trim())) {
                throw new RuntimeException("No items with id " + id + " was deleted");
            }

            List<ItemDto> articleItems = objectMapper.readValue(
                    response.body(), new TypeReference<>() {
                    });

            return articleItems;
        } catch (IOException e) {
            logRequestFailure("Delete ArticleItem with id " + id, request, e);
            throw new RuntimeException("Unable to delete ArticleItem with id " + id, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Delete ArticleItem with id " + id, request, e);
            throw new RuntimeException("Interrupted while deleting ArticleItem with id " + id, e);
        }
    }

    private HttpRequest createDeleteRequest(String uriPath) {
        String url = apiBaseUrl + uriPath;
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .headers(apiKeyParameter, apiKey, "Prefer", "return=representation")
                .build();
    }

    @Override
    public List<ArticleItem> findAllByDatePassedExpirationDate(Date date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllByDatePassedExpirationDate'");
    }

    @Cacheable(value = "articles", key = "#id")
    @Override
    public ArticleItem findById(Long id) {
        String uriPath = basePath + "?id=eq." + id;
        HttpRequest request = createGetRequest(uriPath);

        return getArticleItem(request);
    }

    private @Nullable ArticleItem getArticleItem(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new RuntimeException("Unable to find ArticleItem. Return code: " + response.statusCode());
            }

            List<ItemDto> articleItems = objectMapper.readValue(
                    response.body(), new TypeReference<>() {
                    });

            return articleItems.stream()
                    .findFirst()
                    .map(item -> modelMapper.map(item, ArticleItem.class))
                    .orElse(null);
        } catch (IOException e) {
            logRequestFailure("Find ArticleItem", request, e);
            throw new RuntimeException("Unable to find ArticleItem", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Find ArticleItem", request, e);
            throw new RuntimeException("Interrupted while finding ArticleItem", e);
        }
    }

    @Override
    public ArticleItem findByBarcode(String barcode) {
        String uriPath = basePath + "?barcode=eq." + barcode;

        HttpRequest request = createGetRequest(uriPath);
        return getArticleItem(request);
    }

    @Override
    public ArticleItem findByQrCode(String qrCode) {
        String uriPath = basePath + "?qr_code=eq." + qrCode;

        HttpRequest request = createGetRequest(uriPath);
        return getArticleItem(request);
    }

    @Override
    public ArticleItem findByExpirationDate(Date expirationDate) {
        String uriPath = basePath + "?expiration_date=eq." + expirationDate.toInstant().toString();

        HttpRequest request = createGetRequest(uriPath);
        return getArticleItem(request);
    }

    @Override
    public ArticleItem findByBarcodeAndExpirationDate(String barcode, Date expirationDate) {
        String uriPath = basePath + "?barcode=eq." + barcode;
        // Date format 2026-10-20T22:00:00+00:00
        uriPath += "&expiration_date=eq." + expirationDate.toInstant().toString();

        HttpRequest request = createGetRequest(uriPath);
        ArticleItem articleItem = getArticleItem(request);

        if (articleItem == null) {
            articleItem = findByBarcode(barcode);
        }
        return articleItem;
    }

    @Cacheable(value = "articles", key = "'all'")
    @Override
    public List<ArticleItem> findAll() {
        String uriPath = "/rest/v1/article_items";

        HttpRequest request = createGetRequest(uriPath);

        return getArticleItems(request);
    }

    private List<ArticleItem> getArticleItems(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new RuntimeException(String.format("Request %s %s failed. Return code: %d, body: %s",
                        request.method(), request.uri(), response.statusCode(), response.body()));
            }

            String body = response.body();

            List<ItemDto> itemDtos = objectMapper.readValue(body,
                    new TypeReference<>() {
                    });

            return modelMapper.map(itemDtos, new TypeToken<List<ArticleItem>>() {
            }.getType());
        } catch (IOException e) {
            logRequestFailure("Find all ArticleItems", request, e);
            throw new RuntimeException("Unable to find all ArticleItems", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Find all ArticleItems", request, e);
            throw new RuntimeException("Interrupted while finding all ArticleItems", e);
        }
    }

    @Override
    @CacheEvict(value = "articles", allEntries = true)
    public CreateItemResponse save(ArticleItem articleItem) {
        // Insert first, so the existing items are only marked as replaced once the new one exists.
        ItemDto saved = insertItems(List.of(articleItem)).getFirst();

        long[] replacedIds = StringUtils.isBlank(articleItem.getBarcode())
                ? new long[0]
                : markExpiredAsReplaced(articleItem.getBarcode(), List.of(saved.getId()));

        return new CreateItemResponse(saved, replacedIds);
    }

    @Override
    @CacheEvict(value = "articles", allEntries = true)
    public List<ArticleItem> saveAll(List<ArticleItem> articleItems) {
        if (articleItems.isEmpty()) {
            return List.of();
        }

        // PostgREST inserts the whole array in one statement, so either all items are saved or none are.
        List<ItemDto> savedItems = insertItems(articleItems);

        List<Long> savedIds = savedItems.stream().map(ItemDto::getId).toList();

        articleItems.stream()
                .map(ArticleItem::getBarcode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(barcode -> markExpiredAsReplaced(barcode, savedIds));

        return savedItems.stream()
                .map(item -> modelMapper.map(item, ArticleItem.class))
                .toList();
    }

    private List<ItemDto> insertItems(List<ArticleItem> articleItems) {
        List<CreateItemDto> createItemDtos = articleItems.stream()
                .map(this::toCreateItemDto)
                .toList();

        HttpRequest request = createPostRequest(basePath, convertBodyToString(createItemDtos));

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                logger.error("Failed to save ArticleItems. Status code: {}, Response body: {}", response.statusCode(),
                        response.body());
                throw new RuntimeException("Failed to save ArticleItems. Status code: " + response.statusCode());
            }

            // "Prefer: return=representation" makes the database return the saved rows as an array, including the id.
            List<ItemDto> savedItems = objectMapper.readValue(response.body(),
                    new TypeReference<>() {
                    });

            if (savedItems.size() != articleItems.size()) {
                throw new RuntimeException(String.format("Expected %d saved article items, but got %d",
                        articleItems.size(), savedItems.size()));
            }
            return savedItems;
        } catch (IOException e) {
            logRequestFailure("Save ArticleItems", request, e);
            throw new RuntimeException("Failed to save ArticleItems", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logRequestInterrupted("Save ArticleItems", request, e);
            throw new RuntimeException("Interrupted while saving ArticleItems", e);
        }
    }

    private CreateItemDto toCreateItemDto(ArticleItem articleItem) {
        Date createdAt = articleItem.getCreatedAt() != null ? articleItem.getCreatedAt() : new Date();

        return CreateItemDto.builder()
                .articleName(articleItem.getArticleName())
                .createdAt(createdAt.toInstant().atOffset(ZoneOffset.UTC))
                .expirationDate(articleItem.getExpirationDate())
                .barcode(articleItem.getBarcode())
                .qrCode(articleItem.getQrCode())
                .active(articleItem.isActive())
                .expired(articleItem.isExpired())
                .placement(articleItem.getPlacement())
                .build();
    }

    /**
     * Marks the expired, not yet replaced items with the given barcode as replaced.
     * The newly saved items are excluded, in case they already have passed their expiration date.
     *
     * @return the ids of the items that were marked as replaced
     */
    private long[] markExpiredAsReplaced(String barcode, List<Long> newItemIds) {
        String excludedIds = newItemIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String uriPath = basePath
                + "?barcode=eq." + URLEncoder.encode(barcode, StandardCharsets.UTF_8)
                + "&replaced=eq.false"
                + "&expiration_date=lt." + Instant.now()
                + "&id=not.in.(" + excludedIds + ")";

        HttpRequest request = createPatchRequest(uriPath, "{\"replaced\":true}");

        return getArticleItems(request).stream()
                .mapToLong(ArticleItem::getId)
                .toArray();
    }

    @Override
    public List<ArticleItem> findAllByExpirationDateAndReplaced(Date expirationDate, Boolean replaced) {

        String uriPath = basePath;

        if (expirationDate != null) {
            uriPath += "?expiration_date=eq." + expirationDate.toInstant().toString();
        }

        if (replaced != null) {
            if (uriPath.endsWith("article_items")) {
                uriPath += "?";
            } else {
                uriPath += "&";
            }
            uriPath += "replaced=eq." + replaced;
        }

        HttpRequest request = createGetRequest(uriPath);

        return getArticleItems(request);
    }
}
