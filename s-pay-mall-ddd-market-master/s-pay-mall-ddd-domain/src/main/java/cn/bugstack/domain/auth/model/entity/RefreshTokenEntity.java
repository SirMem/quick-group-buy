package cn.bugstack.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenEntity {

    private String openid;
    private String tokenHash;
    private String status;
    private Date expireTime;

}
