package no.kjetil.preparednessapi.features.articleitem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

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
}