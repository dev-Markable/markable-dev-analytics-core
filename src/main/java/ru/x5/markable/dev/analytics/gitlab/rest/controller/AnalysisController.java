package ru.x5.markable.dev.analytics.gitlab.rest.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.x5.markable.dev.analytics.gitlab.interactor.AnalysisInteractor;
import ru.x5.markable.dev.analytics.gitlab.mapper.AuthorStatsMapper;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisResponse;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisInteractor analysisInteractor;
    private final AuthorStatsMapper mapper;

    @PostMapping
    public ResponseEntity<List<AnalysisResponse>> start(@RequestBody AnalysisRequest request) {
        return ResponseEntity.ok().body(mapper.toDto(analysisInteractor.startAnalysis(request)));
    }
}
