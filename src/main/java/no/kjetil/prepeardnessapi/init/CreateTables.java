package no.kjetil.prepeardnessapi.init;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import jakarta.annotation.PostConstruct;
import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CreateTables {

    private static Logger logger = LoggerFactory.getLogger(CreateTables.class);

    private final AmazonDynamoDB amazonDynamoDB;
    private final ArticleItemRepository articleItemRepository;

    private DynamoDBMapper dynamoDBMapper;

    public CreateTables(AmazonDynamoDB amazonDynamoDB, ArticleItemRepository articleItemRepository) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.articleItemRepository = articleItemRepository;

        dynamoDBMapper = new DynamoDBMapper(this.amazonDynamoDB);

        List<String> tableClassesNames = getDynamoDbTableClasses();

        for(String tableName : tableClassesNames) {
            boolean tableExists = doesTableExist(tableName);

            if(!tableExists) {
                createTable(tableName);
            }
        }
    }

    private void createTable(String tableName) {
        try {
            CreateTableRequest createTableRequest = dynamoDBMapper.generateCreateTableRequest(ArticleItem.class);
            createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            amazonDynamoDB.createTable(createTableRequest);
            logger.info("Table {} created.", tableName);
        } catch (Exception e) {
            logger.error("Failed to create table: {}", tableName);
            logger.error("Reason: {}", e);
        }
    }

    private boolean doesTableExist(String tableName) {
        try {

            DescribeTableRequest describeTableRequest = new DescribeTableRequest(tableName);
            DescribeTableResult describeTableResult = amazonDynamoDB.describeTable(describeTableRequest);
            if (describeTableResult != null) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Could not find the requested table: " + tableName);
        }
        return false;
    }

    @PostConstruct
    public void init() {
        logger.info("Creating tables");


    }

    private List<String> getDynamoDbTableClasses() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(DynamoDBTable.class));

        Set<BeanDefinition> beanDefinitionSet = provider
                .findCandidateComponents("no.kjetil.prepeardnessapi");
        List<String> annotatedBeans = new ArrayList<>();
        for (BeanDefinition bd : beanDefinitionSet) {
            if(bd instanceof AnnotatedBeanDefinition) {
                Map<String, Object> annotAttributeMap = ((AnnotatedBeanDefinition) bd)
                        .getMetadata()
                        .getAnnotationAttributes(DynamoDBTable.class.getCanonicalName());
                annotatedBeans.add(annotAttributeMap.get("tableName").toString());
            }
        }

        return annotatedBeans;
    }
}
