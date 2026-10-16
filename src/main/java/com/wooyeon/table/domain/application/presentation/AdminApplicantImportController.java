package com.wooyeon.table.domain.application.presentation;

import com.wooyeon.table.domain.application.application.dto.ApplicantImportResult;
import com.wooyeon.table.domain.application.application.service.ApplicantImportService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import com.wooyeon.table.global.response.ApiResponse;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/imports")
public class AdminApplicantImportController {

    private final ApplicantImportService applicantImportService;

    /** 신청자 엑셀(가상데이터) 업로드 → 사용자·프로필·신청·회차 적재 */
    @PostMapping(value = "/applicants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ApplicantImportResult> importApplicants(@RequestPart("file") MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return ApiResponse.ok(applicantImportService.importApplicants(input));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMPORT_FAILED, e.getMessage());
        }
    }
}
