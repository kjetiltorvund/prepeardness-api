package no.kjetil.preparednessapi.features.articleitem.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "supabase")
public class ItemServiceProperties {
    private String url;
    private String apiKey;


    public ItemServiceProperties() {
    }

    public ItemServiceProperties(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }


}
