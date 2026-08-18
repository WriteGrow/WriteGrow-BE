package com.example.writegrow.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param dailyGoal 아동 홈에 "오늘 작성 0 / N편" 으로 표시되는 하루 목표. 저학년에게 부담이 되지
 *                  않는 선에서 정한다. 늘리려면 이 값만 바꾼다.
 */
@ConfigurationProperties(prefix = "writegrow.writing")
public record WritingProperties(
        int dailyGoal
) {
}
