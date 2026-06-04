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
    public void saveRefreshToken(String openid, String refreshTokenHash, Date expireTime) {
        authTokenDao.insert(AuthRefreshToken.builder()
                .openid(openid)
                .tokenHash(refreshTokenHash)
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
                .status(authRefreshToken.getStatus())
                .expireTime(authRefreshToken.getExpireTime())
                .build();
    }

    @Override
    public boolean changeRefreshTokenStatus(String refreshTokenHash, String oldStatus, String newStatus) {
        return authTokenDao.updateStatus(refreshTokenHash, oldStatus, newStatus);
    }

}
