package cn.bugstack.domain.auth.service;

import cn.bugstack.domain.auth.model.entity.AuthTokenEntity;

public interface IAuthTokenService {

    AuthTokenEntity createToken(String openid);

    String verifyAccessToken(String accessToken);

    AuthTokenEntity refreshToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);

}
