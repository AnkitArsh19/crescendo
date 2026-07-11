package com.crescendo.publicapi.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PublicPage<T>(
        List<T> data,
        @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("next_cursor") String nextCursor
) {
}
