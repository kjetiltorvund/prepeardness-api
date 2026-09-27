package no.kjetil.preparednessapi.features.articleitem.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UpdateArticleDto {
    private Long id;
    @JsonProperty("article_name")
    private String articleName;
    @JsonProperty("created_at")
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
    private OffsetDateTime createdAt;
    @JsonProperty("expiration_date")
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
    private OffsetDateTime expirationDate;
    //private int daysUntilExpired;
    private String barcode;
    @JsonProperty("qr_code")
    private String qrCode;
    private Boolean active;
    private Boolean expired;
    private String placement;
}
