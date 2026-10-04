package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Relies on env variables")
@SpringBootTest 
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvTestInitializer.class)
class ArticleItemServiceImplITest {

    @Autowired 
    private ArticleItemServiceProperties options;

    private ArticleItemService sut;

    @BeforeEach 
    public void setup() {
        sut = new ArticleItemServiceImpl(
            options,
            HttpClient.newHttpClient(), 
            new ModelMapper(), 
            new ObjectMapper());
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
    public void shouldGetItemByBarcodeAndExpirationDate() throws ParseException {
        // Arrange
        String barcode = "416000336108";
        Date expirationDate = Date.from(OffsetDateTime.parse("2026-10-20T22:00:00+00:00").toInstant());

        // Act
        ArticleItem actual = sut.findByBarcodeAndExpirationDate(barcode, expirationDate);

        // Assert
        assertNotNull(actual);
    }
}