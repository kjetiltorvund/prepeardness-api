package no.kjetil.prepeardnessapi.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableDynamoDBRepositories(basePackages = "no.kjetil.prepeardnessapi.features.articleitem.repositories")
public class DynamoDbConfig {
    
    @Autowired
    private DynamoDbProperties dynamoDbProps;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(awsEndpointConfiguration())
                .withCredentials(awsCredentialProvider())
                .build();
    }

    private AwsClientBuilder.EndpointConfiguration awsEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(dynamoDbProps.getEndpoint(), dynamoDbProps.getRegion());
    }

    private AWSStaticCredentialsProvider awsCredentialProvider() {
        return new AWSStaticCredentialsProvider(
                awsCredentials());
    }

    @Bean
    public AWSCredentials awsCredentials() {
        return new BasicAWSCredentials(dynamoDbProps.getAccessKey(), dynamoDbProps.getSecretKey());
        //return new BasicAWSCredentials(dynamoDbProps.getAccessKey(), dynamoDbProps.getSecretKey());
    }

    @Bean
    @Primary
    public DynamoDBMapperConfig dynamoDBMapperConfig() {
        return DynamoDBMapperConfig.DEFAULT;
    }

    @Bean
    @Primary
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB, DynamoDBMapperConfig config) {
        return new DynamoDBMapper(amazonDynamoDB, config);
    }

}
