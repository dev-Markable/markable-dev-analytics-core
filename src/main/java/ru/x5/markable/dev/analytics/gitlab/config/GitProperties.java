package ru.x5.markable.dev.analytics.gitlab.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "git")
public class GitProperties {

    private List<String> repositories;
}