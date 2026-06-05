package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.AuthRefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IAuthTokenDao {

    void insert(AuthRefreshToken authRefreshToken);

    AuthRefreshToken queryByTokenHash(@Param("tokenHash") String tokenHash);

    boolean markUsed(@Param("tokenHash") String tokenHash, @Param("replacedByTokenHash") String replacedByTokenHash);

    boolean revokeActive(@Param("tokenHash") String tokenHash);

    boolean revokeFamily(@Param("tokenFamilyId") String tokenFamilyId);

}
