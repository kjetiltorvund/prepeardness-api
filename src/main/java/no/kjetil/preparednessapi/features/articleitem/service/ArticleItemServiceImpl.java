package no.kjetil.preparednessapi.features.articleitem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ArticleItemServiceImpl implements ArticleItemService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleItemServiceImpl.class);
    private final ModelMapper modelMapper;

    private HttpClient httpClient;
    private String apiBaseUrl;
    private String apiKey;

    public ArticleItemServiceImpl(HttpClient httpClient,
                                  @Value("${supabase.url}") String apiBaseUrl,
                                  @Value("${supabase.apiKey}") String apiKey,
                                  ModelMapper modelMapper) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
        this.modelMapper = modelMapper;
    }

    @Override
    public ArticleItem createArticleItem(ArticleItem articleItem) {
        return null;
    }

    @Override
    public ArticleItem readArticleItemById(long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiBaseUrl + "/rest/v1/article_items?id=eq." + id))
                .timeout(Duration.ofSeconds(30))
                .headers(
                        "apiKey", apiKey
                ).build();

        logger.info("Sending request to URL: {}", request.uri().toString());

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public ArticleItem updateArticleItem(ArticleItem articleItem) {
        return null;
    }

    @Override
    public void deleteArticleItemById(long id) {

    }
}
