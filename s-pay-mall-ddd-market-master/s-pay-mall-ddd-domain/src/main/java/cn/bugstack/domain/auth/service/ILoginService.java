package cn.bugstack.domain.auth.service;

import cn.bugstack.domain.auth.model.entity.WechatPollEntity;
import cn.bugstack.domain.auth.model.entity.WechatQrCodeEntity;

import java.io.IOException;

public interface ILoginService {

    String createQrCodeTicket() throws Exception;

    String createQrCodeTicket(String sceneStr) throws Exception;

    WechatQrCodeEntity createWechatQrCode() throws Exception;

    String checkLogin(String ticket);

    String checkLogin(String ticket, String sceneStr);

    WechatPollEntity pollWechatLogin(String qrCodeId);

    void saveLoginState(String ticket, String openid) throws IOException;

}
