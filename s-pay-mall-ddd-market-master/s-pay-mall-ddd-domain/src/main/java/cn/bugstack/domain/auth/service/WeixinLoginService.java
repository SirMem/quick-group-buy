package cn.bugstack.domain.auth.service;

import cn.bugstack.domain.auth.adapter.port.ILoginPort;
import cn.bugstack.domain.auth.model.entity.WechatPollEntity;
import cn.bugstack.domain.auth.model.entity.WechatQrCodeEntity;
import cn.bugstack.domain.auth.model.valobj.WechatPollStatusVO;
import cn.bugstack.domain.auth.model.valobj.WeixinQrCodeTicketVO;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class WeixinLoginService implements ILoginService {

    private static final int WECHAT_QR_CODE_EXPIRE_SECONDS = 300;

    @Resource
    private ILoginPort loginPort;
    @Resource
    private Cache<String, String> openidToken;
    @Resource
    private Cache<String, String> wechatQrCodeTicket;

    @Override
    public String createQrCodeTicket() throws Exception {
        return loginPort.createQrCodeTicket();
    }

    @Override
    public String createQrCodeTicket(String sceneStr) throws Exception {
        String ticket = loginPort.createQrCodeTicket(sceneStr);
        // 保存浏览器指纹信息和ticket映射关系
        openidToken.put(sceneStr, ticket);
        return ticket;
    }

    @Override
    public WechatQrCodeEntity createWechatQrCode() throws Exception {
        String qrCodeId = UUID.randomUUID().toString().replace("-", "");
        WeixinQrCodeTicketVO qrCodeTicket = loginPort.createQrCodeTicket(qrCodeId, WECHAT_QR_CODE_EXPIRE_SECONDS);
        wechatQrCodeTicket.put(qrCodeId, qrCodeTicket.getTicket());

        return WechatQrCodeEntity.builder()
                .qrCodeId(qrCodeId)
                .qrCodeUrl(qrCodeTicket.getUrl())
                .expiresIn(qrCodeTicket.getExpireSeconds())
                .build();
    }

    @Override
    public String checkLogin(String ticket) {
        return openidToken.getIfPresent(ticket);
    }

    @Override
    public String checkLogin(String ticket, String sceneStr) {
        String cacheTicket = openidToken.getIfPresent(sceneStr);
        if (StringUtils.isBlank(cacheTicket) || !cacheTicket.equals(ticket)) return null;
        return checkLogin(ticket);
    }

    @Override
    public WechatPollEntity pollWechatLogin(String qrCodeId) {
        String ticket = wechatQrCodeTicket.getIfPresent(qrCodeId);
        if (StringUtils.isBlank(ticket)) {
            return WechatPollEntity.builder()
                    .status(WechatPollStatusVO.EXPIRED.getCode())
                    .build();
        }

        String openid = openidToken.getIfPresent(ticket);
        if (StringUtils.isBlank(openid)) {
            return WechatPollEntity.builder()
                    .status(WechatPollStatusVO.WAITING.getCode())
                    .build();
        }

        return WechatPollEntity.builder()
                .status(WechatPollStatusVO.SCANNED.getCode())
                .build();
    }

    @Override
    public void saveLoginState(String ticket, String openid) throws IOException {
        // 保存登录信息
        openidToken.put(ticket, openid);
        // 发送模板消息
        loginPort.sendLoginTemplate(openid);
    }

}
