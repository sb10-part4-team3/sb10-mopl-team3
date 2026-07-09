package com.example.sb10_MoPl_team3.batch.admin;

import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobExecutionDto;
import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobExecutionPageResponse;
import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobSummaryDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/batch")
public class AdminBatchController {

  private final AdminBatchService adminBatchService;

  @GetMapping("/jobs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<BatchJobSummaryDto>> listJobs() {
    return ResponseEntity.ok(adminBatchService.listJobs());
  }

  @GetMapping("/jobs/{jobName}/executions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BatchJobExecutionPageResponse<BatchJobExecutionDto>> listExecutions(
      @PathVariable String jobName,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(adminBatchService.listExecutions(jobName, page, size));
  }
}
