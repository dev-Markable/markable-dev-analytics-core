package ru.x5.markable.dev.analytics.gitlab.interactor;

import java.text.MessageFormat;
import lombok.AllArgsConstructor;
import ru.x5.markable.dev.analytics.commons.exceptions.MessageTemplate;

@AllArgsConstructor
public enum Message implements MessageTemplate {
    ANALYZE_REPOSITORY_ERROR("При обновлении репозитория {0} возникла ошибка"),
    GIT_ERROR("Ошибка выполнения git команды"),
    STATISTICS_SAVE_ERROR("Ошибка сохранения статистики анализа");

    private final String template;

    @Override
    public String getText(Object... args) {
        return String.format(template, args);
    }
}
