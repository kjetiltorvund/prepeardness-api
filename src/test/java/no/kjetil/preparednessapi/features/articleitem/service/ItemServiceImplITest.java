package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Relies on env variables")
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvTestInitializer.class)
class ItemServiceImplITest {

    @Autowired
    private ItemServiceProperties options;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemService sut;

    @BeforeEach
    public void setup() {
        sut = new ItemServiceImpl(
                options,
                HttpClient.newHttpClient(),
                new ModelMapper(),
                objectMapper);
    }


    @Test
    public void shouldUpdateExistingArticle() {

        // Arrange
        ArticleItem testItem = sut.findById(2L);

        assertNotNull(testItem);

        String placement = "Test box";
        testItem.setPlacement(placement);

        // Act
        var updatedItem = sut.updateArticleItem(testItem);

        assertEquals(placement, updatedItem.getPlacement());
    }

    @Test
    public void shouldGetItemByBarcodeAndExpirationDate() {
        // Arrange
        String barcode = "416000336108";
        Date expirationDate = Date.from(OffsetDateTime.parse("2026-10-20T22:00:00+00:00").toInstant());

        // Act
        ArticleItem actual = sut.findByBarcodeAndExpirationDate(barcode, expirationDate);

        // Assert
        assertNotNull(actual);
    }

    @Test
    public void shouldGetAllArticlesFilteredOnExpirationDateAndReplaced() {
        // Arrange
        Date expirationDate = Date.from(OffsetDateTime.parse("2026-10-20T22:00:00+00:00").toInstant());


        // Act
        List<ArticleItem> actual = sut.findAllByExpirationDateAndReplaced(expirationDate, true);

        // Assert
        assertNotNull(actual);
    }

    @Test
    public void shouldGetAllArticlesFilteredOnReplaced() {
        // Act
        List<ArticleItem> actual = sut.findAllByExpirationDateAndReplaced(null, true);

        // Assert
        assertNotNull(actual);
    }

    @Test
    public void shouldDeleteArticle() {
        // Arrange
        ArticleItem articleItem = ArticleItem.builder()
                .articleName("Test item")
                .build();

        CreateItemResponse save = sut.save(articleItem);

        // Act
        sut.deleteArticleItemById(save.article().getId());

        ArticleItem actual = sut.findById(save.article().getId());

        // Assert
        assertNull(actual);
    }

    @Test
    public void shouldThrowExceptionWhenTryingToDeleteNonExistingItem() {
        // Arrange
        long id = Long.MAX_VALUE;

        // Act
        Exception exception = assertThrows(RuntimeException.class, () -> {
            sut.deleteArticleItemById(id);
        });

        String expectedMessage = "No items with id " + id + " was deleted";
        String actualMessage = exception.getMessage();

        // Assert
        assertTrue(actualMessage.contains(expectedMessage));
    }
}