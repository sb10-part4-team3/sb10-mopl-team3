package com.example.sb10_MoPl_team3.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.service.ContentService;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * ContentController 통합 테스트.
 *
 * <p>{@code @SpringBootTest}로 전체 애플리케이션 컨텍스트(보안 필터 체인, 예외 처리, 직렬화 등)를 로드하고
 * {@code @AutoConfigureMockMvc}로 실제 HTTP 레이어를 MockMvc로 검증합니다.
 * ContentService는 서비스 단위 테스트의 관심사이므로 목 처리합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.batch.job.enabled=false")
class ContentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ContentService contentService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private S3Client s3Client;

    private static final UUID CONTENT_ID = UUID.randomUUID();

    // ========================
    // GET /api/contents/{id}
    // ========================

    @Test
    void find_정상_조회_시_200과_ContentDto_반환() throws Exception {
        given(contentService.getContent(CONTENT_ID)).willReturn(sampleDto());

        mockMvc.perform(get("/api/contents/{id}", CONTENT_ID)
                .with(authentication(userAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(CONTENT_ID.toString()))
            .andExpect(jsonPath("$.type").value("MOVIE"))
            .andExpect(jsonPath("$.title").value("테스트 영화"))
            .andExpect(jsonPath("$.tags[0]").value("액션"))
            .andExpect(jsonPath("$.averageRating").value(4.5))
            .andExpect(jsonPath("$.watcherCount").value(100));
    }

    @Test
    void find_존재하지_않는_ID_조회_시_404와_에러응답_반환() throws Exception {
        given(contentService.getContent(CONTENT_ID))
            .willThrow(new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        mockMvc.perform(get("/api/contents/{id}", CONTENT_ID)
                .with(authentication(userAuth())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(ErrorCode.CONTENT_NOT_FOUND.getMessage()))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void find_미인증_접근_시_401_반환() throws Exception {
        mockMvc.perform(get("/api/contents/{id}", CONTENT_ID))
            .andExpect(status().isUnauthorized());
    }

    // ===========================
    // POST /api/contents (등록)
    // ===========================

    @Test
    void create_ADMIN_권한으로_정상_등록_시_201_반환() throws Exception {
        given(contentService.create(any(), any())).willReturn(sampleDto());

        mockMvc.perform(multipart("/api/contents")
                .part(jsonPart("request", """
                    {"type":"MOVIE","title":"테스트 영화","description":"설명","tags":["액션"]}
                    """))
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(CONTENT_ID.toString()))
            .andExpect(jsonPath("$.type").value("MOVIE"))
            .andExpect(jsonPath("$.title").value("테스트 영화"));
    }

    @Test
    void create_USER_권한으로_접근_시_403과_ACCESS_DENIED_반환() throws Exception {
        mockMvc.perform(multipart("/api/contents")
                .part(jsonPart("request", """
                    {"type":"MOVIE","title":"테스트 영화","tags":[]}
                    """))
                .with(csrf())
                .with(authentication(userAuth())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.status").value(403));

        then(contentService).should(never()).create(any(), any());
    }

    @Test
    void create_미인증_접근_시_401_반환() throws Exception {
        mockMvc.perform(multipart("/api/contents")
                .part(jsonPart("request", """
                    {"type":"MOVIE","title":"테스트 영화","tags":[]}
                    """))
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_type_누락_시_400과_INVALID_INPUT_VALUE_반환() throws Exception {
        mockMvc.perform(multipart("/api/contents")
                .part(jsonPart("request", """
                    {"title":"테스트 영화","tags":[]}
                    """))
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.details.type").exists());
    }

    @Test
    void create_title_누락_시_400과_INVALID_INPUT_VALUE_반환() throws Exception {
        mockMvc.perform(multipart("/api/contents")
                .part(jsonPart("request", """
                    {"type":"MOVIE","tags":[]}
                    """))
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.details.title").exists());
    }

    // ===============================
    // PATCH /api/contents/{id} (수정)
    // ===============================

    @Test
    void update_ADMIN_권한으로_정상_수정_시_200과_ContentDto_반환() throws Exception {
        ContentDto updated = new ContentDto(
            CONTENT_ID, ContentType.MOVIE, "수정된 제목", "수정된 설명",
            null, List.of("액션"), 4.5, 10, 100L
        );
        given(contentService.updateContent(eq(CONTENT_ID), any())).willReturn(updated);

        mockMvc.perform(patch("/api/contents/{id}", CONTENT_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"title":"수정된 제목","description":"수정된 설명","tags":null}
                    """)
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("수정된 제목"))
            .andExpect(jsonPath("$.description").value("수정된 설명"));
    }

    @Test
    void update_USER_권한으로_접근_시_403과_ACCESS_DENIED_반환() throws Exception {
        mockMvc.perform(patch("/api/contents/{id}", CONTENT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"수정\",\"description\":null,\"tags\":null}")
                .with(csrf())
                .with(authentication(userAuth())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.status").value(403));

        then(contentService).should(never()).updateContent(any(), any());
    }

    @Test
    void update_존재하지_않는_ID_수정_시_404_반환() throws Exception {
        given(contentService.updateContent(eq(CONTENT_ID), any()))
            .willThrow(new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        mockMvc.perform(patch("/api/contents/{id}", CONTENT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"수정\",\"description\":null,\"tags\":null}")
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"));
    }

    // ================================
    // DELETE /api/contents/{id} (삭제)
    // ================================

    @Test
    void delete_ADMIN_권한으로_정상_삭제_시_200_반환() throws Exception {
        mockMvc.perform(delete("/api/contents/{id}", CONTENT_ID)
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isOk());

        then(contentService).should().deleteContent(CONTENT_ID);
    }

    @Test
    void delete_USER_권한으로_접근_시_403과_ACCESS_DENIED_반환() throws Exception {
        mockMvc.perform(delete("/api/contents/{id}", CONTENT_ID)
                .with(csrf())
                .with(authentication(userAuth())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.status").value(403));

        then(contentService).should(never()).deleteContent(any());
    }

    @Test
    void delete_존재하지_않는_ID_삭제_시_404_반환() throws Exception {
        willThrow(new BusinessException(ErrorCode.CONTENT_NOT_FOUND))
            .given(contentService).deleteContent(CONTENT_ID);

        mockMvc.perform(delete("/api/contents/{id}", CONTENT_ID)
                .with(csrf())
                .with(authentication(adminAuth())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"));
    }

    // =======================
    // GET /api/contents (목록)
    // =======================

    @Test
    void findContents_정상_목록_조회_시_200과_CursorResponse_반환() throws Exception {
        CursorResponse<ContentDto> response = new CursorResponse<>(
            List.of(sampleDto()), null, null, false, 1L, "createdAt", "ASC"
        );
        given(contentService.getContents(any(), any(), any(), any())).willReturn(response);

        mockMvc.perform(get("/api/contents")
                .param("limit", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "ASC")
                .with(authentication(userAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("테스트 영화"))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("ASC"));
    }

    @Test
    void findContents_필수_파라미터_누락_시_400과_INVALID_INPUT_VALUE_반환() throws Exception {
        mockMvc.perform(get("/api/contents")
                .with(authentication(userAuth())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.details.parameter").exists());
    }

    // ========================
    // helpers
    // ========================

    private ContentDto sampleDto() {
        return new ContentDto(
            CONTENT_ID, ContentType.MOVIE, "테스트 영화", "설명",
            null, List.of("액션"), 4.5, 10, 100L
        );
    }

    private Authentication adminAuth() {
        return new UsernamePasswordAuthenticationToken(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private Authentication userAuth() {
        return new UsernamePasswordAuthenticationToken(
            "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private MockPart jsonPart(String name, String json) {
        MockPart part = new MockPart(name, json.strip().getBytes());
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }
}
