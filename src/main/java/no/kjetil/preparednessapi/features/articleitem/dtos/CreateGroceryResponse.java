package no.kjetil.preparednessapi.features.articleitem.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateGroceryResponse(
        ItemDto article,
        @JsonProperty("replace_ids") long[] replaceIds) {
}
