package com.example.sb10_MoPl_team3.content.dto;

import com.example.sb10_MoPl_team3.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ContentCreateRequest(
    @NotNull(message = "type은 필수입니다")
    ContentType type,
    @NotBlank(message = "title은 필수입니다")
    String title,
    String description,
    List<String> tags
) {

}
