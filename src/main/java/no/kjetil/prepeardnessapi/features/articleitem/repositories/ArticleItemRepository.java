package no.kjetil.prepeardnessapi.features.articleitem.repositories;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;

import org.socialsignin.spring.data.dynamodb.repository.DynamoDBCrudRepository;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@EnableScan
@Repository("articleItemRepository")
public interface ArticleItemRepository extends DynamoDBCrudRepository<ArticleItem, String> {
    //@Query("select * from ArticleItem as ai where ai.expirationDate < :date")
    //List<ArticleItem> findAllByExpirationDateAfter(Date date);
}
