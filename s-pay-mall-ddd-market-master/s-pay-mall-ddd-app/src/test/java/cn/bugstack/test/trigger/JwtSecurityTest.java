package cn.bugstack.test.trigger;

import cn.bugstack.domain.auth.model.entity.WechatQrCodeEntity;
import cn.bugstack.domain.auth.service.IAuthTokenService;
import cn.bugstack.domain.auth.service.ILoginService;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.trigger.http.AliPayController;
import cn.bugstack.trigger.http.WechatAuthController;
import cn.bugstack.trigger.http.WeixinPortalController;
import cn.bugstack.trigger.security.JwtAuthenticationEntryPoint;
import cn.bugstack.trigger.security.JwtAuthenticationFilter;
import cn.bugstack.trigger.security.SecurityConfig;
import com.alipay.api.AlipayClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JWT 安全回归测试
 * <p>
 * 验证边界：
 * 1. 受保护订单入口未携带 token 时返回 HTTP 401
 * 2. 携带有效 token 时使用 JWT subject/openid，忽略请求体伪造的 userId
 * 3. 放行路径（回调、认证入口）不被 JWT 过滤器拦截
 * 4. 无效 token 返回 401
 */
@WebMvcTest({AliPayController.class, WechatAuthController.class, WeixinPortalController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "weixin.config.originalid=test-original-id",
        "weixin.config.token=test-token",
})
class JwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IOrderService orderService;

    @MockBean
    private AlipayClient alipayClient;

    @MockBean
    private IAuthTokenService authTokenService;

    @MockBean
    private ILoginService loginService;

    // ==================== 受保护入口：未携带 token = 401 ====================

    @Test
    void createPayOrder_NoToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/alipay/create_pay_order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"fake-user\",\"productId\":\"100001\",\"marketType\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void queryUserOrderList_NoToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/alipay/query_user_order_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"fake-user\",\"lastId\":null,\"pageSize\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refundOrder_NoToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/alipay/refund_order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"fake-user\",\"orderId\":\"test-order-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activePayNotify_NoToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/alipay/active_pay_notify")
                        .param("outTradeNo", "test-order-123"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 携带有效 token：使用 JWT subject ====================

    @Test
    void createPayOrder_WithValidToken_ShouldUseJwtSubject() throws Exception {
        when(authTokenService.verifyAccessToken("valid-token")).thenReturn("openid-from-token");

        PayOrderEntity mockOrder = PayOrderEntity.builder()
                .userId("openid-from-token")
                .orderId("order-123")
                .payUrl("https://pay.example.com")
                .build();
        when(orderService.createOrder(any(ShopCartEntity.class))).thenReturn(mockOrder);

        mockMvc.perform(post("/api/v1/alipay/create_pay_order")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"fake-user\",\"productId\":\"100001\",\"marketType\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        // 验证：orderService.createOrder 收到的 ShopCartEntity.userId = JWT subject，
        // 而不是请求体伪造的 "fake-user"
        verify(orderService).createOrder(argThat(cart ->
                "openid-from-token".equals(cart.getUserId()) &&
                        "100001".equals(cart.getProductId())
        ));
    }

    // ==================== 无效 token = 401 ====================

    @Test
    void createPayOrder_WithInvalidToken_ShouldReturn401() throws Exception {
        when(authTokenService.verifyAccessToken("invalid-token")).thenReturn(null);

        mockMvc.perform(post("/api/v1/alipay/create_pay_order")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"fake-user\",\"productId\":\"100001\",\"marketType\":0}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 放行路径：不受 JWT 拦截 ====================

    @Test
    void groupBuyNotify_NoToken_ShouldNotBeBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/alipay/group_buy_notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":\"test-team\",\"outTradeNoList\":[\"test-order-123\"]}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(401, status, "group_buy_notify 不应因 JWT 返回 401");
                });
    }

    @Test
    void alipayNotifyUrl_NoToken_ShouldNotBeBlocked() throws Exception {
        // 该入口会因 AlipaySignature 验签失败抛出 AlipayApiException（静态方法，不可 mock），
        // MockMvc 包装为 NestedServletException，但不应因 JWT 返回 401
        try {
            mockMvc.perform(post("/api/v1/alipay/alipay_notify_url")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("trade_status", "TRADE_SUCCESS")
                            .param("out_trade_no", "test-order-123")
                            .param("gmt_payment", "2024-06-01 12:00:00")
                            .param("sign", "test-sign")
                            .param("charset", "utf-8"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertNotEquals(401, status, "alipay_notify_url 不应因 JWT 返回 401");
                    });
        } catch (Exception e) {
            // AlipaySignature 验签异常会包装为 NestedServletException，但不应是 401 相关
            assertFalse(e.getMessage() != null && e.getMessage().contains("401"),
                    "alipay_notify_url 不应因 JWT 返回 401");
        }
    }

    @Test
    void weixinPortalReceive_NoToken_ShouldNotBeBlocked() throws Exception {
        // GET 验签：无 token 不应被 JWT 拦截；会因缺 signature 参数返回 400 而非 401
        mockMvc.perform(get("/api/v1/weixin/portal/receive"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(401, status, "weixin/portal/receive GET 不应因 JWT 返回 401");
                });

        // POST 消息：无 token 不应被 JWT 拦截；会因缺参数抛异常返回 500 而非 401
        mockMvc.perform(post("/api/v1/weixin/portal/receive")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml><ToUserName>test</ToUserName></xml>"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(401, status, "weixin/portal/receive POST 不应因 JWT 返回 401");
                });
    }

    @Test
    void authWechatQrcode_NoToken_ShouldNotBeBlocked() throws Exception {
        when(loginService.createWechatQrCode()).thenReturn(WechatQrCodeEntity.builder()
                .qrCodeId("test-qr-id")
                .qrCodeUrl("https://weixin.qq.com/qr/test")
                .expiresIn(120L)
                .build());

        mockMvc.perform(post("/api/auth/wechat/qrcode"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(401, status, "/api/auth/wechat/qrcode 不应因 JWT 返回 401");
                });
    }

}
