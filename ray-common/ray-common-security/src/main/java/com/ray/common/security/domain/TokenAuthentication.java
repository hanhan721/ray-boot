package com.ray.common.security.domain;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
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

    private final String accessToken;
    private final LoginUser loginUser;
    private final @Nullable UserAuthorization authorization;

    public TokenAuthentication(String accessToken, LoginUser loginUser,
                               Collection<? extends GrantedAuthority> authorities) {
        this(accessToken, loginUser, authorities, null);
    }

    public TokenAuthentication(String accessToken, LoginUser loginUser,
                               Collection<? extends GrantedAuthority> authorities,
                               @Nullable UserAuthorization authorization) {
        super(authorities);
        this.accessToken = accessToken;
        this.loginUser = loginUser;
        this.authorization = authorization;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.accessToken;
    }

    @Override
    public Object getPrincipal() {
        return this.loginUser;
    }

}
