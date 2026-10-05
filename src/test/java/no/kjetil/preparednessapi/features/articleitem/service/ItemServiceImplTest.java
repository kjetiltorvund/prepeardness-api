package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.config.JacksonConfig;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateGroceryResponse;
import no.kjetil.preparednessapi.features.articleitem.dtos.ItemDto;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ItemServiceImplTest {

    @Test
    public void shouldGetArticleItemById() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("[{\"id\":4,\"article_name\":\"Pepperonini\",\"placement\":\"Loftet\"}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem articleItem = itemService.readArticleItemById(4);

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

        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem articleItem = itemService.findById(4L);

        assertNotNull(articleItem);
        assertEquals(Instant.parse("2026-01-25T22:25:32.687Z"), articleItem.getCreatedAt().toInstant());
    }

    private ItemServiceProperties getOptions() {
        return new ItemServiceProperties("https://example.invalid", "test-api-key");
    }

    @Test
    public void shouldPostToCreateArticleItem() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn("[{\"id\":7,\"article_name\":\"Pepperonini\",\"placement\":\"Loftet\",\"active\":true}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);

        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem newArticleItem = getNewArticleItem();
        ArticleItem createdArticleItem = itemService.createArticleItem(newArticleItem);

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
        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                objectMapper);

        CreateGroceryResponse result = itemService.save(ArticleItem.builder()
                .articleName("Pepperonini")
                .expirationDate(Date.from(Instant.parse("2030-01-02T03:04:05Z")))
                .qrCode("qr-123")
                .build());

        assertEquals(8, result.article().getId());
        assertEquals(0, result.replaceIds().length);

        // Without a barcode there is nothing to replace, so only the insert is sent.
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(BodyHandler.class));

        String requestBody = readRequestBody(requestCaptor.getValue());

        var body = objectMapper.readTree(requestBody);
        assertTrue(body.isArray());
        assertEquals(1, body.size());

        var json = body.get(0);
        assertEquals("Pepperonini", json.get("article_name").asText());
        assertEquals("2030-01-02T03:04:05.000+00:00", json.get("expiration_date").asText());
        assertEquals("qr-123", json.get("qr_code").asText());
        assertFalse(json.has("id"));
        assertFalse(json.has("articleName"));
        assertFalse(json.has("expirationDate"));
        assertFalse(json.has("qrCode"));
    }

    @Test
    public void shouldMarkExpiredItemsWithSameBarcodeAsReplacedWhenSaving() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> insertResponse = mock(HttpResponse.class);
        when(insertResponse.statusCode()).thenReturn(201);
        when(insertResponse.body()).thenReturn("[{\"id\":9,\"article_name\":\"Pepperonini\",\"barcode\":\"416000336108\"}]");
        @SuppressWarnings("unchecked")
        HttpResponse<String> patchResponse = mock(HttpResponse.class);
        when(patchResponse.statusCode()).thenReturn(200);
        when(patchResponse.body()).thenReturn("[{\"id\":3,\"replaced\":true},{\"id\":5,\"replaced\":true}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
                .thenReturn(insertResponse)
                .thenReturn(patchResponse);

        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        CreateGroceryResponse result = itemService.save(ArticleItem.builder()
                .articleName("Pepperonini")
                .barcode("416000336108")
                .build());

        assertEquals(9, result.article().getId());
        assertArrayEquals(new long[]{3, 5}, result.replaceIds());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(BodyHandler.class));

        HttpRequest insertRequest = requestCaptor.getAllValues().get(0);
        HttpRequest patchRequest = requestCaptor.getAllValues().get(1);
        assertEquals("POST", insertRequest.method());
        assertEquals("PATCH", patchRequest.method());
        assertEquals("/rest/v1/article_items", patchRequest.uri().getPath());
        assertReplacePatchQuery(patchRequest, "416000336108", "9");
        assertEquals("{\"replaced\":true}", readRequestBody(patchRequest));
    }

    @Test
    public void shouldInsertAllItemsInOneRequestAndMarkExpiredItemsAsReplacedWhenSavingAll() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> insertResponse = mock(HttpResponse.class);
        when(insertResponse.statusCode()).thenReturn(201);
        when(insertResponse.body()).thenReturn("[{\"id\":10,\"article_name\":\"Pepperonini\",\"barcode\":\"111\"},"
                + "{\"id\":11,\"article_name\":\"Pepperonini\",\"barcode\":\"111\"},"
                + "{\"id\":12,\"article_name\":\"Tomatoes\"}]");
        @SuppressWarnings("unchecked")
        HttpResponse<String> patchResponse = mock(HttpResponse.class);
        when(patchResponse.statusCode()).thenReturn(200);
        when(patchResponse.body()).thenReturn("[{\"id\":3,\"replaced\":true}]");
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
                .thenReturn(insertResponse)
                .thenReturn(patchResponse);

        ObjectMapper objectMapper = new JacksonConfig().objectMapper();
        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                objectMapper);

        List<ArticleItem> saved = itemService.saveAll(List.of(
                ArticleItem.builder().articleName("Pepperonini").barcode("111").build(),
                ArticleItem.builder().articleName("Pepperonini").barcode("111").build(),
                ArticleItem.builder().articleName("Tomatoes").build()));

        assertEquals(List.of(10L, 11L, 12L), saved.stream().map(ArticleItem::getId).toList());

        // One insert for all items, and one replace per distinct barcode.
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(BodyHandler.class));

        HttpRequest insertRequest = requestCaptor.getAllValues().get(0);
        assertEquals("POST", insertRequest.method());
        assertEquals(3, objectMapper.readTree(readRequestBody(insertRequest)).size());

        HttpRequest patchRequest = requestCaptor.getAllValues().get(1);
        assertEquals("PATCH", patchRequest.method());
        assertReplacePatchQuery(patchRequest, "111", "10,11,12");
    }

    @Test
    public void shouldNotSendAnyRequestWhenSavingAnEmptyList() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);

        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        assertTrue(itemService.saveAll(List.of()).isEmpty());
        verify(httpClient, never()).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    private void assertReplacePatchQuery(HttpRequest patchRequest, String barcode, String excludedIds) {
        String query = patchRequest.uri().getQuery();
        assertTrue(query.startsWith("barcode=eq." + barcode + "&replaced=eq.false&expiration_date=lt."), query);
        assertTrue(query.endsWith("&id=not.in.(" + excludedIds + ")"), query);

        String expirationLimit = query.replaceAll(".*expiration_date=lt\\.([^&]+)&.*", "$1");
        Instant limit = Instant.parse(expirationLimit);
        assertTrue(Math.abs(Instant.now().toEpochMilli() - limit.toEpochMilli()) < 60_000, expirationLimit);
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

        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper()
        );

        String newPlacementFromTest = "New placement from test";
        ArticleItem existingItem = getNewArticleItem();
        existingItem.setId(7);
        existingItem.setPlacement(newPlacementFromTest);

        ArticleItem result = itemService.updateArticleItem(existingItem);

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

        ItemService itemService = new ItemServiceImpl(
                new ItemServiceProperties("http://localhost", "test-api-key"),
                httpClient,
                modelMapper,
                objectMapper
        );

        ArticleItem articleItem = ArticleItem.builder().id(1L).articleName("Test").build();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.updateArticleItem(articleItem));

        assertTrue(exception.getMessage().contains("Unable to update object"));
        assertTrue(exception.getMessage().contains("422"));
        assertTrue(exception.getMessage().contains(errorBody));

        verify(objectMapper, never()).readValue(any(String.class), any(TypeReference.class));
        verify(modelMapper, never()).map(any(ItemDto.class), eq(ArticleItem.class));
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


        ItemService itemService = new ItemServiceImpl(
                getOptions(),
                httpClient,
                new ModelMapper(),
                new JacksonConfig().objectMapper()
        );

        String barcode = "416000336108";
        Date expirationDate = Date.from(OffsetDateTime.parse("2026-10-20T22:00:00+00:00").toInstant());


        // Act
        itemService.findByBarcodeAndExpirationDate(barcode, expirationDate);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(BodyHandler.class));

        URI uri = requestCaptor.getValue().uri();
        assertEquals(expectedPath, uri.getPath() + "?" + uri.getQuery());
    }
}
