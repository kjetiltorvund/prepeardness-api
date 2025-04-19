package no.kjetil.prepeardnessapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "amazon.aws.dynamodb")
@Data
public class DynamoDbProperties {
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
    private String tableName;
}
