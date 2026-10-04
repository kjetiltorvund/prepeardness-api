package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.config.JacksonConfig;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.UpdateArticleDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArticleItemServiceImplTest {

    @Test
    public void shouldGetArticleItemById() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("[{\"id\":4,\"article_name\":\"Pepperonini\",\"placement\":\"Loftet\"}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem articleItem = articleItemService.readArticleItemById(4);

        assertNotNull(articleItem);
        assertEquals(4, articleItem.getId());
        verify(httpClient).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    public void shouldPreserveCreatedAtWhenGettingArticleItemById() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("[{\"id\":4,\"article_name\":\"Pepperonini\","
                + "\"created_at\":\"2026-01-25T22:25:32.68761+00:00\"}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem articleItem = articleItemService.findById(4L);

        assertNotNull(articleItem);
        assertEquals(Instant.parse("2026-01-25T22:25:32.687Z"), articleItem.getCreatedAt().toInstant());
    }

    private ArticleItemServiceProperties getOptions() {
        return new ArticleItemServiceProperties("https://example.invalid", "test-api-key");
    }

    @Test
    public void shouldPostToCreateArticleItem() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn("[{\"id\":7,\"article_name\":\"Pepperonini\",\"placement\":\"Loftet\",\"active\":true}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem newArticleItem = getNewArticleItem();
        ArticleItem createdArticleItem = articleItemService.createArticleItem(newArticleItem);

        assertNotNull(createdArticleItem);
        assertEquals(7, createdArticleItem.getId());
        verify(httpClient).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    public void shouldUseArticleItemDtoJsonPropertyNamesWhenSaving() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn("[{\"id\":8,\"article_name\":\"Pepperonini\",\"qr_code\":\"qr-123\"}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ObjectMapper objectMapper = new JacksonConfig().objectMapper();
        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                objectMapper);

        ArticleItem savedItem = articleItemService.save(ArticleItem.builder()
                .articleName("Pepperonini")
                .expirationDate(Date.from(Instant.parse("2030-01-02T03:04:05Z")))
                .qrCode("qr-123")
                .build());

        assertEquals(8, savedItem.getId());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(BodyHandler.class));

        String requestBody = readRequestBody(requestCaptor.getValue());

        var json = objectMapper.readTree(requestBody);
        assertEquals("Pepperonini", json.get("article_name").asText());
        assertEquals("2030-01-02T03:04:05.000+00:00", json.get("expiration_date").asText());
        assertEquals("qr-123", json.get("qr_code").asText());
        assertFalse(json.has("articleName"));
        assertFalse(json.has("expirationDate"));
        assertFalse(json.has("qrCode"));
    }

    private String readRequestBody(HttpRequest request) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompletableFuture<String> result = new CompletableFuture<>();

        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                output.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                result.complete(output.toString(StandardCharsets.UTF_8));
            }
        });

        return result.join();
    }

    private ArticleItem getNewArticleItem() {
        return ArticleItem.builder()
                .active(true)
                .articleName("Pepperonini")
                .placement("Loftet")
                .expirationDate(Date.from(Instant.now()))
                .build();
    }

    @Test
    public void shouldUpdateExistingArticleItem() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("[{\"id\":7,\"article_name\":\"Pepperonini\",\"placement\":\"New placement from test\",\"active\":true}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper()
        );

        String newPlacementFromTest = "New placement from test";
        ArticleItem existingItem = getNewArticleItem();
        existingItem.setId(7);
        existingItem.setPlacement(newPlacementFromTest);

        ArticleItem result = articleItemService.updateArticleItem(existingItem);

        assertNotNull(result);
        assertThat(result.getPlacement(), is(newPlacementFromTest));
        verify(httpClient).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    public void shouldThrowOnUpdateWhenApiReturnsErrorPayloadWithCodeField() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        ModelMapper modelMapper = mock(ModelMapper.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(java.net.http.HttpResponse.class);

        String errorBody = "{\"code\":\"PGRST116\",\"message\":\"Update failed\"}";

        when(response.statusCode()).thenReturn(422);
        when(response.body()).thenReturn(errorBody);
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
        when(modelMapper.map(any(ArticleItem.class), eq(UpdateArticleDto.class)))
                .thenReturn(UpdateArticleDto.builder().id(1L).build());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                new ArticleItemServiceProperties("http://localhost", "test-api-key"),
                httpClient,
                modelMapper,
                objectMapper
        );

        ArticleItem articleItem = ArticleItem.builder().id(1L).articleName("Test").build();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> articleItemService.updateArticleItem(articleItem));

        assertTrue(exception.getMessage().contains("Unable to update object"));
        assertTrue(exception.getMessage().contains("422"));
        assertTrue(exception.getMessage().contains(errorBody));

        verify(objectMapper, never()).readValue(any(String.class), any(TypeReference.class));
        verify(modelMapper, never()).map(any(ArticleItemDto.class), eq(ArticleItem.class));
    }

    @Test
    public void shouldCallForBarcodeAndExpirationDateCorrectly() throws IOException, InterruptedException {
        // Arrange
        String expectedPath = "/rest/v1/article_items?barcode=eq.416000336108&expiration_date=eq.2026-10-20T22:00:00Z";

        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(java.net.http.HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("[{\"id\":1,\"barcode\":\"416000336108\"}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);


        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper()
        );

        String barcode = "416000336108";
        Date expirationDate = Date.from(OffsetDateTime.parse("2026-10-20T22:00:00+00:00").toInstant());


        // Act
        articleItemService.findByBarcodeAndExpirationDate(barcode, expirationDate);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(BodyHandler.class));

        URI uri = requestCaptor.getValue().uri();
        assertEquals(expectedPath, uri.getPath() + "?" + uri.getQuery());
    }
}
