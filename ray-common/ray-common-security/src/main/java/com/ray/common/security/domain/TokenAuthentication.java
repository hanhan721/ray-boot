package com.ray.common.security.domain;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * 基于 Token 的认证对象
 */
@Getter
public class TokenAuthentication extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final LoginUser loginUser;
    private final UserAuthorization authorization;

    public TokenAuthentication(String sessionId, LoginUser loginUser,
                               Collection<? extends GrantedAuthority> authorities,
                               UserAuthorization authorization) {
        super(authorities);
        this.sessionId = sessionId;
        this.loginUser = loginUser;
        this.authorization = java.util.Objects.requireNonNull(authorization);
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.loginUser;
    }

}
