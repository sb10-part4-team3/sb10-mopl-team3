package com.example.sb10_MoPl_team3.content.dto;

import java.util.List;

public record ContentUpdateRequest(
    String title,
    String description,
    List<String> tags
) {

}
