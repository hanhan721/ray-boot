package com.ray.framework.web.translate;

import com.ray.common.core.translate.Translator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translator 注册中心：启动时收集所有 {@link Translator} 实现，按 type 分派。
 */
@Component
public class TranslateRegistry {

    private final Map<String, Translator> translators;

    public TranslateRegistry(List<Translator> translatorList) {
        Map<String, Translator> map = new HashMap<>(translatorList.size() * 2);
        for (Translator t : translatorList) {
            Translator existing = map.put(t.type(), t);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate Translator type=" + t.type() + ": "
                                + existing.getClass().getName() + " vs " + t.getClass().getName());
            }
        }
        this.translators = Map.copyOf(map);
    }

    public Translator require(String type) {
        Translator t = translators.get(type);
        if (t == null) {
            throw new IllegalStateException("No Translator registered for type=" + type);
        }
        return t;
    }

    public boolean has(String type) {
        return translators.containsKey(type);
    }
}
