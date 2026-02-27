package ru.x5.markable.dev.analytics.gitlab.exception;

import lombok.Getter;

@Getter
public class RepositoryAnalysisException extends AnalysisException {

  private final String repository;

  public RepositoryAnalysisException(String repository, Throwable cause) {
    super("Failed to analyze repository: " + repository, cause);
    this.repository = repository;
  }

}
