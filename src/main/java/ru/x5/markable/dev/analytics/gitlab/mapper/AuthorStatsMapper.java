package ru.x5.markable.dev.analytics.gitlab.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AuthorStats;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisResponse;

@Mapper(componentModel = "spring")
public interface AuthorStatsMapper {
    @Mapping(target = "email", source = "email")
    @Mapping(target = "mergeCommits", source = "mergeCommits")
    @Mapping(target = "commits", source = "commits")
    @Mapping(target = "added", source = "addedLines")
    @Mapping(target = "deleted", source = "deletedLines")
    @Mapping(target = "testAdded", source = "testAddedLines")
    AnalysisResponse toDto(AuthorStats source);

    List<AnalysisResponse> toDto(List<AuthorStats> source);


}
