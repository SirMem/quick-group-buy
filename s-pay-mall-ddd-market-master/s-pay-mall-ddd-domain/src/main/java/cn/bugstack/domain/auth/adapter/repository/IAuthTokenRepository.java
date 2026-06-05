package cn.bugstack.domain.auth.adapter.repository;

import cn.bugstack.domain.auth.model.entity.RefreshTokenEntity;

import java.util.Date;

public interface IAuthTokenRepository {

    void saveRefreshToken(String openid, String refreshTokenHash, String tokenFamilyId, Date expireTime);

    RefreshTokenEntity queryRefreshToken(String refreshTokenHash);

    boolean markRefreshTokenUsed(String refreshTokenHash, String replacedByTokenHash);

    boolean revokeActiveRefreshToken(String refreshTokenHash);

    boolean revokeTokenFamily(String tokenFamilyId);

}
