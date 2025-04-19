package no.kjetil.prepeardnessapi.features.articleitem;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import no.kjetil.prepeardnessapi.App;
import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {App.class})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "amazon.aws.dynamodb.endpoint=http://localhost:8000",
        "amazon.aws.dynamodb.accesskey=dynamodblocal",
        "amazon.aws.dynamodb.secretkey="})
public class ArticleItemDynamoDbTest {
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    ArticleItemRepository articleItemRepository;

    @Before
    public void setup() {

        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);

        boolean tableExists = false;
        String tableName = "article_items";
        try {

            DescribeTableRequest describeTableRequest = new DescribeTableRequest(tableName);
            DescribeTableResult describeTableResult = amazonDynamoDB.describeTable(describeTableRequest);
            if (describeTableResult != null) {
                tableExists = true;
            }
        } catch (Exception e) {
            System.out.println("Could not find the requested table: " + tableName);
        }

        if (!tableExists) {
            try {
                CreateTableRequest createTableRequest = dynamoDBMapper.generateCreateTableRequest(ArticleItem.class);
                createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
                amazonDynamoDB.createTable(createTableRequest);
            } catch (Exception e) {
                System.out.println("Failed to create table.");
                e.printStackTrace();
            }
        }

        dynamoDBMapper.batchDelete(articleItemRepository.findAll());
    }

    @After
    public void tearDown() {
        ListTablesResult listTablesResult = amazonDynamoDB.listTables();
        List<String> tableNames = listTablesResult.getTableNames();

        for (String tableName : tableNames) {
            try {
                DeleteTableResult deleteTableResult = amazonDynamoDB.deleteTable(tableName);

                TableDescription tableDescription = deleteTableResult.getTableDescription();
            } catch (Exception e) {
                System.out.println("Failed to delete the tables: " + tableName);
                e.printStackTrace();
            }
        }
    }

    @Test
    public void shouldGetArticleItemById() {
        // Given
        ArticleItem articleItem = new ArticleItem();
        articleItem.setArticleName("Test Article");
        articleItem.setActive(true);
        articleItem.setExpired(false);
        articleItem.setExpirationDate(new Date(LocalDate.now().plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond()));
        articleItem.setBarcode("1234567890123");

        // When
        articleItemRepository.save(articleItem);

        // Then
        ArticleItem foundArticleItem = articleItemRepository.findById(articleItem.getId()).orElse(null);
        assertThat(foundArticleItem, notNullValue());
    }

}
