package no.kjetil.prepeardnessapi.config;

import lombok.Data;

@Data
public class DynamoDbProperties {
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
    private String tableName;
}
