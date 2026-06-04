package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AuthTokenResponseDTO {

    public static final String TOKEN_TYPE_BEARER = "Bearer";
    public static final Long ACCESS_TOKEN_EXPIRES_IN = 1800L;
    public static final Long REFRESH_TOKEN_EXPIRES_IN = 2592000L;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String openid;
    private String authMethod;

}
