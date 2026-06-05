package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.auth.adapter.repository.IAuthTokenRepository;
import cn.bugstack.domain.auth.model.entity.RefreshTokenEntity;
import cn.bugstack.infrastructure.dao.IAuthTokenDao;
import cn.bugstack.infrastructure.dao.po.AuthRefreshToken;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Date;

@Repository
public class AuthTokenRepository implements IAuthTokenRepository {

    @Resource
    private IAuthTokenDao authTokenDao;

    @Override
    public void saveRefreshToken(String openid, String refreshTokenHash, String tokenFamilyId, Date expireTime) {
        authTokenDao.insert(AuthRefreshToken.builder()
                .openid(openid)
                .tokenHash(refreshTokenHash)
                .tokenFamilyId(tokenFamilyId)
                .status("ACTIVE")
                .expireTime(expireTime)
                .build());
    }

    @Override
    public RefreshTokenEntity queryRefreshToken(String refreshTokenHash) {
        AuthRefreshToken authRefreshToken = authTokenDao.queryByTokenHash(refreshTokenHash);
        if (authRefreshToken == null) return null;

        return RefreshTokenEntity.builder()
                .openid(authRefreshToken.getOpenid())
                .tokenHash(authRefreshToken.getTokenHash())
                .tokenFamilyId(authRefreshToken.getTokenFamilyId())
                .status(authRefreshToken.getStatus())
                .expireTime(authRefreshToken.getExpireTime())
                .revokedAt(authRefreshToken.getRevokedAt())
                .replacedByTokenHash(authRefreshToken.getReplacedByTokenHash())
                .build();
    }

    @Override
    public boolean markRefreshTokenUsed(String refreshTokenHash, String replacedByTokenHash) {
        return authTokenDao.markUsed(refreshTokenHash, replacedByTokenHash);
    }

    @Override
    public boolean revokeActiveRefreshToken(String refreshTokenHash) {
        return authTokenDao.revokeActive(refreshTokenHash);
    }

    @Override
    public boolean revokeTokenFamily(String tokenFamilyId) {
        return authTokenDao.revokeFamily(tokenFamilyId);
    }

}
