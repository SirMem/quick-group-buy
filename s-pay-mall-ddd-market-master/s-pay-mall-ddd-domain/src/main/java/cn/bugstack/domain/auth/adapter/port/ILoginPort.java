package cn.bugstack.domain.auth.adapter.port;

import cn.bugstack.domain.auth.model.valobj.WeixinQrCodeTicketVO;

import java.io.IOException;

public interface ILoginPort {

    String createQrCodeTicket() throws IOException;

    String createQrCodeTicket(String sceneStr) throws IOException;

    WeixinQrCodeTicketVO createQrCodeTicket(String sceneStr, int expireSeconds) throws IOException;

    void sendLoginTemplate(String openid) throws IOException;

}
