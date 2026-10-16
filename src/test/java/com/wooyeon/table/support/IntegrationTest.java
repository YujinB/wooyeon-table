package com.wooyeon.table.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Spring 컨텍스트 + Docker MySQL로 도는 통합 테스트. 같은 설정끼리 컨텍스트·컨테이너를 공유한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(MySqlContainerConfig.class)
public @interface IntegrationTest {
}
