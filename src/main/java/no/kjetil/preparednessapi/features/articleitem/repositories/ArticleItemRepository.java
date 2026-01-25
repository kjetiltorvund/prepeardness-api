package no.kjetil.preparednessapi.features.articleitem.repositories;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

import java.util.Date;
import java.util.List;

//@Repository("articleItemRepository")
public interface ArticleItemRepository
        //extends JpaRepository<ArticleItem, Long>
{
    //@Query("select ai from ArticleItem ai where ai.expirationDate < :date")
    List<ArticleItem> findAllByDatePassedExpirationDate(Date date);
}
