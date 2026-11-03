package com.ray.system.service;

/** 账号维度的登录失败累计与短时冷却。 */
public interface LoginAttemptService {
    void checkAllowed(String username);

    void recordFailure(String username);

    void clearFailures(String username);
}
