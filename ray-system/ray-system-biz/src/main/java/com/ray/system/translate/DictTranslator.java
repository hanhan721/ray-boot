package com.ray.system.translate;

import com.ray.common.core.translate.TranslateMeta;
import com.ray.common.core.translate.Translator;
import com.ray.system.api.entity.SysDictData;
import com.ray.system.service.DictDataService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * type="dict" 的翻译器：按 {@link TranslateMeta#value()} 作为 dictType 调用
 * {@link DictDataService#listByDictType(String)}（内部已有 Spring Cache），
 * 构建 dictValue → dictLabel 映射。
 */
@Component
public class DictTranslator implements Translator {

    private final DictDataService dictDataService;

    public DictTranslator(DictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @Override
    public String type() {
        return "dict";
    }

    @Override
    public Map<Object, String> batchTranslate(Set<Object> keys, TranslateMeta meta) {
        String dictType = meta.value();
        if (!StringUtils.hasText(dictType)) {
            throw new IllegalStateException(
                    "@Translate(type=\"dict\") requires value=dictType");
        }
        List<SysDictData> items = dictDataService.listByDictType(dictType);
        if (CollectionUtils.isEmpty(items)) {
            return Map.of();
        }
        Map<String, String> valueToLabel = new HashMap<>(items.size() * 2);
        for (SysDictData item : items) {
            if (item.getDictValue() != null && item.getDictLabel() != null) {
                valueToLabel.put(item.getDictValue(), item.getDictLabel());
            }
        }
        Map<Object, String> result = new HashMap<>(keys.size() * 2);
        for (Object key : keys) {
            String label = valueToLabel.get(String.valueOf(key));
            if (label != null) {
                result.put(key, label);
            }
        }
        return result;
    }
}
