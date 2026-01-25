package no.kjetil.preparednessapi.features.articleitem.service;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

public interface ArticleItemService {
    ArticleItem createArticleItem(ArticleItem articleItem);

    ArticleItem readArticleItemById(long id);

    ArticleItem updateArticleItem(ArticleItem articleItem);

    void deleteArticleItemById(long id);
}
