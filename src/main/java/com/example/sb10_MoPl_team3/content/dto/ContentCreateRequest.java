package com.example.sb10_MoPl_team3.content.dto;

import com.example.sb10_MoPl_team3.content.ContentType;
import java.util.List;

public record ContentCreateRequest(
    ContentType type,
    String title,
    String description,
    List<String> tags
) {

}
