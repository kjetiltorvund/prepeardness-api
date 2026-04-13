package no.kjetil.preparednessapi.features.articleitem.service;

import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.utils.DotenvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        new ModelMapper());

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
        new ModelMapper());

        ArticleItem newArticleItem = ArticleItem.builder()
        .active(true)
        .articleName("Pepperonini")
        .placement("Loftet")
        .expirationDate(Date.from(Instant.now()))
        .build();

        // Act
        ArticleItem createdArticleItem = articleItemService.createArticleItem(newArticleItem);
        
        // Assert
        assertNotNull(createdArticleItem);
    }

}