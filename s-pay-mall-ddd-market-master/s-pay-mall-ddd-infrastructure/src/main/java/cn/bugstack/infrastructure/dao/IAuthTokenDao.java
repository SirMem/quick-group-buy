package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.AuthRefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IAuthTokenDao {

    void insert(AuthRefreshToken authRefreshToken);

    AuthRefreshToken queryByTokenHash(@Param("tokenHash") String tokenHash);

    boolean updateStatus(@Param("tokenHash") String tokenHash, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus);

}
