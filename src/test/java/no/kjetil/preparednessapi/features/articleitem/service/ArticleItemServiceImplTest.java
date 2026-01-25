package no.kjetil.preparednessapi.features.articleitem.service;

import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.utils.DotenvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ContextConfiguration(initializers = DotenvTestInitializer.class)
class ArticleItemServiceImplTest {

    @BeforeAll
    public static void setup() {
    }

    @Test
    public void shouldGetArticleItemById() {
        ArticleItemService articleItemService = new ArticleItemServiceImpl(HttpClient.newHttpClient(),
                DotenvLoader.get("SUPABASE_URL"),
                DotenvLoader.get("SUPABASE_SECRET_API_KEY"), new ModelMapper());

        ArticleItem articleItem = articleItemService.readArticleItemById(4);

        assertNotNull(articleItem);
    }

}