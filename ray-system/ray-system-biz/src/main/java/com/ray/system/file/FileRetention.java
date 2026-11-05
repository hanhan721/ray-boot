package com.ray.system.file;

import com.ray.system.service.ParamService;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.time.Duration;

/** 统一解析文件保留期，缺失使用默认值，非法配置必须明确失败。 */
@Component
@RequiredArgsConstructor
public class FileRetention {
    private final ParamService parameters;

    /** 每轮清理开始时读取完整策略快照，避免同一轮不同批次采用不同期限。 */
    public Policy read() {
        return new Policy(this.value("file.unbound.expireHours", 24, 1),
                this.value("file.object.retentionDays", 3, 1), this.orphanDays());
    }

    /** 上传开始时单独读取孤儿保留期，作为本次上传允许发布的最长耗时。 */
    public int orphanDays() { return this.value("file.orphan.retentionDays", 7, 7); }

    /** 仅参数不存在时回退默认值；格式错误、溢出或低于下限均拒绝继续。 */
    private int value(String key, int fallback, int minimum) {
        // 1.区分缺失和错误配置，空串等非法值不能悄悄采用默认值。
        var raw = parameters.getStringByKey(key);
        if (raw == null) return fallback;
        try {
            // 2.限定为十进制非负整数，并检查 int 范围及参数自己的业务下限。
            if (!raw.matches("[0-9]+")) throw new NumberFormatException();
            int value = Integer.parseInt(raw);
            if (value < minimum) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法文件清理参数: " + key);
        }
    }

    /** 本轮策略快照：未绑定期限按小时，对象和孤儿保留期按天。 */
    public record Policy(int unboundHours, int objectDays, int orphanDays) {
        public Duration orphanRetention() { return Duration.ofDays(orphanDays); }
    }
}
