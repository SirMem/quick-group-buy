package cn.bugstack.domain.auth.service;

import cn.bugstack.domain.auth.adapter.repository.IAuthTokenRepository;
import cn.bugstack.domain.auth.model.entity.AuthTokenEntity;
import cn.bugstack.domain.auth.model.entity.RefreshTokenEntity;
import cn.bugstack.domain.auth.model.valobj.AuthMethodVO;
import cn.bugstack.domain.auth.model.valobj.RefreshTokenStatusVO;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class AuthTokenService implements IAuthTokenService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String JWT_TOKEN_TYPE_CLAIM = "typ";
    private static final String JWT_ACCESS_TOKEN_TYPE = "access";
    private static final long ACCESS_TOKEN_EXPIRES_IN = 1800L;
    private static final long REFRESH_TOKEN_EXPIRES_IN = 2592000L;
    private static final int REFRESH_TOKEN_BYTES = 48;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.jwt.secret:quick-group-buy-auth-secret}")
    private String jwtSecret;

    @Resource
    private IAuthTokenRepository authTokenRepository;

    @Override
    public AuthTokenEntity createToken(String openid) {
        return createToken(openid, createTokenFamilyId());
    }

    @Override
    public String verifyAccessToken(String accessToken) {
        if (StringUtils.isBlank(accessToken)) return null;

        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withClaim(JWT_TOKEN_TYPE_CLAIM, JWT_ACCESS_TOKEN_TYPE)
                    .build();
            DecodedJWT decodedJWT = verifier.verify(accessToken);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException e) {
            log.warn("accessToken 校验失败", e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenEntity refreshToken(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) return null;

        String refreshTokenHash = hashRefreshToken(refreshToken);
        RefreshTokenEntity refreshTokenEntity = authTokenRepository.queryRefreshToken(refreshTokenHash);
        if (refreshTokenEntity == null) return null;

        if (!RefreshTokenStatusVO.ACTIVE.getCode().equals(refreshTokenEntity.getStatus())) {
            revokeTokenFamily(refreshTokenEntity.getTokenFamilyId());
            return null;
        }

        if (refreshTokenEntity.getExpireTime() == null || refreshTokenEntity.getExpireTime().before(new Date())) {
            return null;
        }

        String newAccessToken = createAccessToken(refreshTokenEntity.getOpenid());
        String newRefreshToken = createRefreshToken();
        String newRefreshTokenHash = hashRefreshToken(newRefreshToken);

        boolean changed = authTokenRepository.markRefreshTokenUsed(refreshTokenHash, newRefreshTokenHash);
        if (!changed) {
            revokeTokenFamily(refreshTokenEntity.getTokenFamilyId());
            return null;
        }

        authTokenRepository.saveRefreshToken(
                refreshTokenEntity.getOpenid(),
                newRefreshTokenHash,
                refreshTokenEntity.getTokenFamilyId(),
                createRefreshTokenExpireTime());

        return buildAuthTokenEntity(refreshTokenEntity.getOpenid(), newAccessToken, newRefreshToken);
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) return;
        authTokenRepository.revokeActiveRefreshToken(hashRefreshToken(refreshToken));
    }

    private AuthTokenEntity createToken(String openid, String tokenFamilyId) {
        String accessToken = createAccessToken(openid);
        String refreshToken = createRefreshToken();
        authTokenRepository.saveRefreshToken(openid, hashRefreshToken(refreshToken), tokenFamilyId, createRefreshTokenExpireTime());
        return buildAuthTokenEntity(openid, accessToken, refreshToken);
    }

    private AuthTokenEntity buildAuthTokenEntity(String openid, String accessToken, String refreshToken) {
        return AuthTokenEntity.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(ACCESS_TOKEN_EXPIRES_IN)
                .refreshExpiresIn(REFRESH_TOKEN_EXPIRES_IN)
                .openid(openid)
                .authMethod(AuthMethodVO.WECHAT_QR.getCode())
                .build();
    }

    private String createAccessToken(String openid) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + ACCESS_TOKEN_EXPIRES_IN * 1000L);
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        return JWT.create()
                .withSubject(openid)
                .withIssuedAt(now)
                .withExpiresAt(expireTime)
                .withClaim(JWT_TOKEN_TYPE_CLAIM, JWT_ACCESS_TOKEN_TYPE)
                .sign(algorithm);
    }

    private String createRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Date createRefreshTokenExpireTime() {
        return new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRES_IN * 1000L);
    }

    private String createTokenFamilyId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String hashRefreshToken(String refreshToken) {
        return DigestUtils.sha256Hex(refreshToken);
    }

    private void revokeTokenFamily(String tokenFamilyId) {
        if (StringUtils.isNotBlank(tokenFamilyId)) {
            authTokenRepository.revokeTokenFamily(tokenFamilyId);
        }
    }

}
