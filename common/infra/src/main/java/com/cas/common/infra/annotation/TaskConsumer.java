package com.cas.common.infra.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * TaskConsumer 커스텀 어노테이션
 * 
 * Kafka 메시지를 처리하는 Consumer 클래스에 사용됩니다.
 * @Controller와 유사한 역할을 하며, Spring의 @Component를 포함합니다.
 * 
 * 사용 예시:
 * <pre>
 * {@literal @}TaskConsumer
 * public class TestTaskConsumer extends BaseTaskConsumer&lt;TestTask&gt; {
 *     
 *     {@literal @}Override
 *     protected void processTask(TestTask task) {
 *         // 실제 작업 처리 로직
 *     }
 * }
 * </pre>
 * 
 * @see org.springframework.stereotype.Controller
 * @see org.springframework.stereotype.Service
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TaskConsumer {
    
    /**
     * Consumer의 이름 (선택 사항)
     * Spring Bean의 이름으로 사용됩니다.
     */
    String value() default "";
    
    /**
     * Consumer 설명 (문서화 목적)
     */
    String description() default "";
}

