package com.example.sb10_MoPl_team3.batch.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobExecutionPageResponse;
import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobSummaryDto;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminBatchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminBatchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminBatchService adminBatchService;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private JwtSessionValidator jwtSessionValidator;

  @Test
  @DisplayName("관리자는 배치 Job 요약 목록을 조회할 수 있다")
  void listJobs_success() throws Exception {
    given(adminBatchService.listJobs()).willReturn(List.of(
        new BatchJobSummaryDto("tmdbMovieSyncJob", 1L, BatchStatus.COMPLETED, "COMPLETED", null, null)
    ));

    mockMvc.perform(get("/api/admin/batch/jobs")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].jobName").value("tmdbMovieSyncJob"))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"));
  }

  @Test
  @DisplayName("일반 사용자는 배치 Job 요약 목록을 조회할 수 없다")
  void listJobs_forbidden() throws Exception {
    mockMvc.perform(get("/api/admin/batch/jobs")
            .with(user("user").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 배치 Job 요약 목록을 조회할 수 없다")
  void listJobs_unauthenticated() throws Exception {
    mockMvc.perform(get("/api/admin/batch/jobs"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("관리자는 특정 Job의 실행 이력을 조회할 수 있다")
  void listExecutions_success() throws Exception {
    given(adminBatchService.listExecutions(eq("tmdbMovieSyncJob"), any(Integer.class), any(Integer.class)))
        .willReturn(new BatchJobExecutionPageResponse<>(List.of(), 0, 20, false, 0L));

    mockMvc.perform(get("/api/admin/batch/jobs/{jobName}/executions", "tmdbMovieSyncJob")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("일반 사용자는 특정 Job의 실행 이력을 조회할 수 없다")
  void listExecutions_forbidden() throws Exception {
    mockMvc.perform(get("/api/admin/batch/jobs/{jobName}/executions", "tmdbMovieSyncJob")
            .with(user("user").roles("USER")))
        .andExpect(status().isForbidden());
  }
}
