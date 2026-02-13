package ru.x5.markable.dev.analytics.gitlab.rest.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisResponse;
import ru.x5.markable.dev.analytics.gitlab.service.AnalysisService;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<AnalysisResponse> start(@RequestBody AnalysisRequest request) {

        UUID id = analysisService.startAnalysis(request);

        return ResponseEntity.status(HttpStatus.OK).body(AnalysisResponse.builder()
                .analysisId(id)
                .status("RUNNING")
                .build()
        );
    }
}
