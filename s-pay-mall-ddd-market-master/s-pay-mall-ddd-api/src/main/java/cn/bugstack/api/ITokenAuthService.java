package cn.bugstack.api;

import cn.bugstack.api.dto.AuthTokenResponseDTO;
import cn.bugstack.api.dto.RefreshTokenRequestDTO;
import cn.bugstack.api.response.Response;

public interface ITokenAuthService {

    Response<AuthTokenResponseDTO> refreshToken(RefreshTokenRequestDTO requestDTO);

    Response<Boolean> logout(RefreshTokenRequestDTO requestDTO);

}
