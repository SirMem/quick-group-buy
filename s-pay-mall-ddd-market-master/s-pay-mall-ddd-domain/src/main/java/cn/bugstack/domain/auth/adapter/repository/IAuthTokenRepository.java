package cn.bugstack.domain.auth.adapter.repository;

import cn.bugstack.domain.auth.model.entity.RefreshTokenEntity;

import java.util.Date;

public interface IAuthTokenRepository {

    void saveRefreshToken(String openid, String refreshTokenHash, Date expireTime);

    RefreshTokenEntity queryRefreshToken(String refreshTokenHash);

    boolean changeRefreshTokenStatus(String refreshTokenHash, String oldStatus, String newStatus);

}
