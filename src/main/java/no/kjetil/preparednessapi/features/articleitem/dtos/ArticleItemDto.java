package no.kjetil.preparednessapi.features.articleitem.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Data
public final class ArticleItemDto {
    private long id;
    @JsonProperty("article_name")
    private String articleName;
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonProperty("expiration_date")
    private Date expirationDate;
    private String barcode;
    @JsonProperty("qr_code")
    private String qrCode;
    private Boolean active;
    private Boolean expired;
    private String placement;
}
