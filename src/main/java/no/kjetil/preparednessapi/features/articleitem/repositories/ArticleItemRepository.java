package no.kjetil.preparednessapi.features.articleitem.repositories;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("articleItemRepository")
public interface ArticleItemRepository extends JpaRepository<ArticleItem, Long>
{
    //@Query("select ai from ArticleItem ai where ai.expirationDate < :date")
    List<ArticleItem> findAllByDatePassedExpirationDate(Date date);
}
