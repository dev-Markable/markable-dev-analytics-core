package ru.x5.markable.dev.analytics.gitlab.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.x5.markable.dev.analytics.gitlab.interactor.AnalysisInteractor;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisInteractor analysisInteractor;

    @PostMapping
    public ResponseEntity<Void> start(@RequestBody AnalysisRequest request) {
        analysisInteractor.startAnalysis(request);

        return ResponseEntity.ok().build();
    }
}
