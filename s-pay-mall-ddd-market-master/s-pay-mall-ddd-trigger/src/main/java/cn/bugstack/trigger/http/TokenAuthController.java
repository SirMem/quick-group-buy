package cn.bugstack.trigger.http;

import cn.bugstack.api.ITokenAuthService;
import cn.bugstack.api.dto.AuthTokenResponseDTO;
import cn.bugstack.api.dto.RefreshTokenRequestDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.auth.model.entity.AuthTokenEntity;
import cn.bugstack.domain.auth.service.IAuthTokenService;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/auth/")
public class TokenAuthController implements ITokenAuthService {

    @Resource
    private IAuthTokenService authTokenService;

    @PostMapping("token/refresh")
    @Override
    public Response<AuthTokenResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO requestDTO) {
        if (requestDTO == null || StringUtils.isBlank(requestDTO.getRefreshToken())) {
            return Response.<AuthTokenResponseDTO>builder()
                    .code(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo())
                    .build();
        }

        try {
            AuthTokenEntity authTokenEntity = authTokenService.refreshToken(requestDTO.getRefreshToken());
            if (authTokenEntity == null) {
                return Response.<AuthTokenResponseDTO>builder()
                        .code(Constants.ResponseCode.NO_LOGIN.getCode())
                        .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                        .build();
            }

            log.info("刷新 Token 成功");
            return Response.<AuthTokenResponseDTO>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(AuthTokenResponseDTO.builder()
                            .accessToken(authTokenEntity.getAccessToken())
                            .refreshToken(authTokenEntity.getRefreshToken())
                            .tokenType(authTokenEntity.getTokenType())
                            .expiresIn(authTokenEntity.getExpiresIn())
                            .refreshExpiresIn(authTokenEntity.getRefreshExpiresIn())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("刷新 Token 失败", e);
            return Response.<AuthTokenResponseDTO>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @PostMapping("logout")
    @Override
    public Response<Boolean> logout(@RequestBody RefreshTokenRequestDTO requestDTO) {
        if (requestDTO == null || StringUtils.isBlank(requestDTO.getRefreshToken())) {
            return Response.<Boolean>builder()
                    .code(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo())
                    .build();
        }

        try {
            authTokenService.revokeRefreshToken(requestDTO.getRefreshToken());
            log.info("退出登录成功");
            return Response.<Boolean>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(Boolean.TRUE)
                    .build();
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return Response.<Boolean>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
