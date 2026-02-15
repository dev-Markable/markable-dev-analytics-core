package ru.x5.markable.dev.analytics.gitlab.exception;

import lombok.Getter;

@Getter
public class GitExecutionException extends AnalysisException {

  private final String command;

  public GitExecutionException(String command, Throwable cause) {
    super("Git command failed: " + command, cause);
    this.command = command;
  }
}
