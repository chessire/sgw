package sgw.common.web.condition;

import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnPropertySimple.class.getName());
        if (attrs == null) {
            return true;
        }
        String prefix = (String) attrs.get("prefix");
        String name = (String) attrs.get("name");
        String havingValue = (String) attrs.get("havingValue");
        boolean matchIfMissing = (Boolean) attrs.get("matchIfMissing");

        String key = buildKey(prefix, name);
        String propertyValue = env.getProperty(key);

        if (propertyValue == null) {
            return matchIfMissing;
        }
        if (!StringUtils.hasText(havingValue)) {
            return true;
        }
        return havingValue.equalsIgnoreCase(propertyValue.trim());
    }

    private String buildKey(String prefix, String name) {
        if (!StringUtils.hasText(prefix)) {
            return name;
        }
        if (prefix.endsWith(".")) {
            return prefix + name;
        }
        return prefix + "." + name;
    }
}
