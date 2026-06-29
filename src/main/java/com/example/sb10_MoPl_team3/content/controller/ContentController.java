package com.example.sb10_MoPl_team3.content.controller;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.dto.ContentUpdateRequest;
import com.example.sb10_MoPl_team3.content.service.ContentService;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/contents")
@RestController
@RequiredArgsConstructor
public class ContentController {

  private final ContentService contentService;

  @GetMapping("/{contentId}")
  public ResponseEntity<ContentDto> find (@PathVariable UUID contentId){
    ContentDto dto = contentService.getContent(contentId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ContentDto> create(
      @RequestPart("request") ContentCreateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
  ){
    ContentDto dto = contentService.create(request, thumbnail);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @PatchMapping("/{contentId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ContentDto> update(@PathVariable UUID contentId,
      @RequestBody ContentUpdateRequest request){
    ContentDto dto = contentService.updateContent(contentId, request);
    return ResponseEntity.ok(dto);
  }

  @DeleteMapping("/{contentId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
    contentService.deleteContent(contentId);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<CursorResponse<ContentDto>> findContents(
      @RequestParam(required = false) String typeEqual,
      @RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) List<String> tagsIn,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam String sortBy,
      @RequestParam String sortDirection
  ) {
    CursorPageRequest pageRequest = new CursorPageRequest(cursor, idAfter, limit, sortBy, sortDirection);
    CursorResponse<ContentDto> response = contentService.getContents(pageRequest, typeEqual, keywordLike, tagsIn);
    return ResponseEntity.ok(response);
  }


}
