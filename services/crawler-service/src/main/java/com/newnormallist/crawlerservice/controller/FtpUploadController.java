package com.newnormallist.crawlerservice.controller;

import com.newnormallist.crawlerservice.util.FtpUploader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



/**
 * FTP 파일 업로드 컨트롤러
 * 
 * 역할:
 * - CSV 파일을 FTP 서버에 업로드
 * - 폴더 구조: /1/am|pm/yyyy-MM-dd_am|pm/stage/
 * - 크롤러 서비스 내부에서 사용하는 FTP 업로드 API
 * 
 * 기능:
 * - POST /api/ftp/upload: CSV 파일 업로드
 * - 디렉터리 자동 생성
 * - 파일 덮어쓰기 지원
 */
@Slf4j
@RestController
@RequestMapping("/api/ftp")
public class FtpUploadController {

    /**
     * CSV 파일 업로드 API (JSON 방식)
     * 
     * @param request 업로드 요청 (경로, 파일명, 내용)
     * @return 업로드 결과
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsv(@RequestBody CsvUploadRequest request) {
        try {
            // FTP 경로 구성: /1 + 상대경로
            String ftpPath = "/1/" + request.getPath();
            
            boolean result = FtpUploader.uploadCsvFile(
                "dev.macacolabs.site",    // FTP 서버 IP
                21,                       // 포트
                "newsone",               // 사용자
                "newsone",               // 비밀번호
                ftpPath,                 // FTP 경로
                request.getFilename(),   // 파일명
                request.getContent()     // CSV 내용
            );

            if (result) {
                log.info("📁 FTP 업로드 성공: {}/{}", ftpPath, request.getFilename());
                return ResponseEntity.ok("업로드 성공");
            } else {
                log.error("📁 FTP 업로드 실패: {}/{}", ftpPath, request.getFilename());
                return ResponseEntity.status(500).body("업로드 실패");
            }
            
        } catch (Exception e) {
            log.error("📁 FTP 업로드 오류: {}, 오류: {}", request.getFilename(), e.getMessage());
            return ResponseEntity.status(500).body("업로드 오류: " + e.getMessage());
        }
    }

    /**
     * 일반 파일 업로드 API (MultipartFile 방식) - 테스트용
     * 
     * @param file 업로드할 파일
     * @param path FTP 상대 경로 (예: "pm/2025-08-19_pm/list")
     * @return 업로드 결과
     */
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path) {
        
        try {
            // FTP 경로 구성: /1 + 상대경로
            String ftpPath = "/1/" + path;
            
            boolean result = FtpUploader.uploadFile(
                "dev.macacolabs.site",    // FTP 서버 IP
                21,                       // 포트
                "newsone",               // 사용자
                "newsone",               // 비밀번호
                ftpPath,                 // FTP 경로
                file                     // 파일
            );

            if (result) {
                log.info("📁 FTP 파일 업로드 성공: {}/{}", ftpPath, file.getOriginalFilename());
                return ResponseEntity.ok("파일 업로드 성공");
            } else {
                log.error("📁 FTP 파일 업로드 실패: {}/{}", ftpPath, file.getOriginalFilename());
                return ResponseEntity.status(500).body("파일 업로드 실패");
            }
            
        } catch (Exception e) {
            log.error("📁 FTP 파일 업로드 오류: {}, 오류: {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(500).body("파일 업로드 오류: " + e.getMessage());
        }
    }

    /**
     * CSV 업로드 요청 DTO
     */
    public static class CsvUploadRequest {
        private String path;      // 상대 경로 (예: "pm/2025-08-19_pm/list")
        private String filename;  // 파일명 (예: "politics_list_2025-08-19-15-26.csv")
        private String content;   // 파일 내용 (CSV 데이터)
        
        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
