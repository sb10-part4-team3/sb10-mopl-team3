package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import java.util.List;


public interface ContentTagService {

  public List<String> syncTags(Content content, List<String> tagNames);

}
