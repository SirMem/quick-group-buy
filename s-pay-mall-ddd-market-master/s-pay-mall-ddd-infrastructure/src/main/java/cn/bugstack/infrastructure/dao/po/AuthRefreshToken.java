package cn.bugstack.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRefreshToken {

    private Long id;
    private String openid;
    private String tokenHash;
    private String tokenFamilyId;
    private String status;
    private Date expireTime;
    private Date revokedAt;
    private String replacedByTokenHash;
    private Date createTime;
    private Date updateTime;

}
