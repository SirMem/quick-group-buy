package cn.bugstack.test.domain.auth;

import cn.bugstack.domain.auth.adapter.repository.IAuthTokenRepository;
import cn.bugstack.domain.auth.model.entity.AuthTokenEntity;
import cn.bugstack.domain.auth.model.entity.RefreshTokenEntity;
import cn.bugstack.domain.auth.model.valobj.RefreshTokenStatusVO;
import cn.bugstack.domain.auth.service.IAuthTokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback
public class AuthTokenServiceTest {

    @Resource
    private IAuthTokenService authTokenService;

    @Resource
    private IAuthTokenRepository authTokenRepository;

    @Test
    public void test_refreshToken_rotation_shouldPersistReplacementAndFamily() {
        String openid = createTestOpenid("rotation");
        AuthTokenEntity originToken = authTokenService.createToken(openid);
        String oldRefreshTokenHash = hashRefreshToken(originToken.getRefreshToken());
        RefreshTokenEntity oldTokenBeforeRefresh = authTokenRepository.queryRefreshToken(oldRefreshTokenHash);

        AuthTokenEntity rotatedToken = authTokenService.refreshToken(originToken.getRefreshToken());

        Assert.assertNotNull(rotatedToken);
        Assert.assertNotEquals(originToken.getRefreshToken(), rotatedToken.getRefreshToken());
        Assert.assertEquals(openid, rotatedToken.getOpenid());
        Assert.assertEquals(openid, authTokenService.verifyAccessToken(rotatedToken.getAccessToken()));

        String newRefreshTokenHash = hashRefreshToken(rotatedToken.getRefreshToken());
        RefreshTokenEntity oldTokenAfterRefresh = authTokenRepository.queryRefreshToken(oldRefreshTokenHash);
        RefreshTokenEntity newRefreshToken = authTokenRepository.queryRefreshToken(newRefreshTokenHash);

        Assert.assertNotNull(oldTokenBeforeRefresh);
        Assert.assertNotNull(oldTokenAfterRefresh);
        Assert.assertNotNull(newRefreshToken);
        Assert.assertEquals(RefreshTokenStatusVO.USED.getCode(), oldTokenAfterRefresh.getStatus());
        Assert.assertNotNull(oldTokenAfterRefresh.getRevokedAt());
        Assert.assertEquals(newRefreshTokenHash, oldTokenAfterRefresh.getReplacedByTokenHash());
        Assert.assertEquals(RefreshTokenStatusVO.ACTIVE.getCode(), newRefreshToken.getStatus());
        Assert.assertEquals(oldTokenBeforeRefresh.getTokenFamilyId(), oldTokenAfterRefresh.getTokenFamilyId());
        Assert.assertEquals(oldTokenBeforeRefresh.getTokenFamilyId(), newRefreshToken.getTokenFamilyId());
    }

    @Test
    public void test_refreshToken_reuse_shouldRevokeTokenFamily() {
        String openid = createTestOpenid("reuse");
        AuthTokenEntity originToken = authTokenService.createToken(openid);
        AuthTokenEntity rotatedToken = authTokenService.refreshToken(originToken.getRefreshToken());
        Assert.assertNotNull(rotatedToken);

        AuthTokenEntity reusedTokenResult = authTokenService.refreshToken(originToken.getRefreshToken());
        AuthTokenEntity familyRevokedResult = authTokenService.refreshToken(rotatedToken.getRefreshToken());

        RefreshTokenEntity rotatedRefreshToken = authTokenRepository.queryRefreshToken(hashRefreshToken(rotatedToken.getRefreshToken()));

        Assert.assertNull(reusedTokenResult);
        Assert.assertNull(familyRevokedResult);
        Assert.assertNotNull(rotatedRefreshToken);
        Assert.assertEquals(RefreshTokenStatusVO.REVOKED.getCode(), rotatedRefreshToken.getStatus());
        Assert.assertNotNull(rotatedRefreshToken.getRevokedAt());
    }

    @Test
    public void test_revokeRefreshToken_shouldBeIdempotent() {
        String openid = createTestOpenid("logout");
        AuthTokenEntity originToken = authTokenService.createToken(openid);
        String refreshTokenHash = hashRefreshToken(originToken.getRefreshToken());

        authTokenService.revokeRefreshToken(originToken.getRefreshToken());
        authTokenService.revokeRefreshToken(originToken.getRefreshToken());

        RefreshTokenEntity refreshToken = authTokenRepository.queryRefreshToken(refreshTokenHash);
        AuthTokenEntity refreshResult = authTokenService.refreshToken(originToken.getRefreshToken());

        Assert.assertNotNull(refreshToken);
        Assert.assertEquals(RefreshTokenStatusVO.REVOKED.getCode(), refreshToken.getStatus());
        Assert.assertNotNull(refreshToken.getRevokedAt());
        Assert.assertNull(refreshResult);
    }

    @Test
    public void test_unknownRefreshToken_shouldReturnNullAndLogoutNoop() {
        String unknownRefreshToken = "unknown-refresh-token-" + UUID.randomUUID();

        AuthTokenEntity refreshResult = authTokenService.refreshToken(unknownRefreshToken);
        authTokenService.revokeRefreshToken(unknownRefreshToken);

        Assert.assertNull(refreshResult);
    }

    private String createTestOpenid(String scene) {
        return "issue3-test-" + scene + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String hashRefreshToken(String refreshToken) {
        return DigestUtils.sha256Hex(refreshToken);
    }

}
