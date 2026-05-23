package com.ray.common.mybatis.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.ray.common.core.util.Snowflake53;

/**
 * 53 位雪花 ID 生成器（MyBatis-Plus 适配）
 * <p>
 * 替换 MyBatis-Plus 默认的 64 位雪花算法，生成 ≤ 53 位的 ID，
 * 确保前端 JavaScript Number 不丢失精度。
 */
public class Snowflake53IdGenerator implements IdentifierGenerator {

    private final Snowflake53 snowflake;

    public Snowflake53IdGenerator(long machineId) {
        this.snowflake = new Snowflake53(machineId);
    }

    @Override
    public Number nextId(Object entity) {
        return snowflake.nextId();
    }
}
