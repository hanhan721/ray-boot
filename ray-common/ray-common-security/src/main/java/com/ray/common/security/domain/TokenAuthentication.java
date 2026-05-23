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

    private final String accessToken;
    private final LoginUser loginUser;

    public TokenAuthentication(String accessToken, LoginUser loginUser,
                               Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.accessToken = accessToken;
        this.loginUser = loginUser;
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
