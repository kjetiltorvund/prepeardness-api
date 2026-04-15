package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.config.JacksonConfig;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.UpdateArticleDto;
import no.kjetil.preparednessapi.utils.DotenvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static java.net.http.HttpResponse.BodyHandler;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ContextConfiguration(initializers = DotenvTestInitializer.class)
class ArticleItemServiceImplTest {

    @BeforeAll
    public static void setup() {
    }

    @Test
    public void shouldGetArticleItemById() {
        // Arrange
        ArticleItemService articleItemService = new ArticleItemServiceImpl(
        getOptions(), 
        HttpClient.newHttpClient(),
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        // Act
        ArticleItem articleItem = articleItemService.readArticleItemById(4);

        // Assert
        assertNotNull(articleItem);
    }

    private ArticleItemServiceProperties getOptions() {
        ArticleItemServiceProperties options = new ArticleItemServiceProperties(
            DotenvLoader.get("SUPABASE_URL"), 
            DotenvLoader.get("SUPABASE_SECRET_API_KEY"));
        return options;
    }

    @Test
    public void shouldPostToCreateArticleItem() {
        // Arrange
        ArticleItemService articleItemService = new ArticleItemServiceImpl(
        getOptions(), 
        HttpClient.newHttpClient(),
                new ModelMapper(),
                new JacksonConfig().objectMapper());

        ArticleItem newArticleItem = getNewArticleItem();

        // Act
        ArticleItem createdArticleItem = articleItemService.createArticleItem(newArticleItem);

        // Assert
        assertNotNull(createdArticleItem);
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
    public void shouldUpdateExistingArticleItem() {
        // Arrange
        ArticleItemService articleItemService = new ArticleItemServiceImpl(
                getOptions(),
                HttpClient.newHttpClient(),
                new ModelMapper(),
                new JacksonConfig().objectMapper()
        );

        Optional<ArticleItem> hasExistingItem = articleItemService.findAll().stream().filter(p -> p.getArticleName().equals("Pepperonini")).findFirst();

        if (hasExistingItem.isEmpty()) {
            ArticleItem newItem = getNewArticleItem();

            articleItemService.createArticleItem(newItem);

            hasExistingItem = articleItemService.findAll().stream().filter(p -> p.getArticleName().equals("Pepperonini")).findFirst();
        }

        ArticleItem existingItem = hasExistingItem.orElse(null);

        String newPlacementFromTest = "New placement from test";
        existingItem.setPlacement(newPlacementFromTest);

        // Act
        ArticleItem result = articleItemService.updateArticleItem(existingItem);

        // Assert
        assertNotNull(result);
        assertThat(result.getPlacement(), is(newPlacementFromTest));
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

}