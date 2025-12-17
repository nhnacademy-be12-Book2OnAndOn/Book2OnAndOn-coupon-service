package com.example.book2onandoncouponservice.handler;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DlqErrorHandler {
    private final DoorayHookClient doorayHookClient;

    @Value("${dooray.service.id}")
    private String serviceId;
    @Value("${dooray.bot.id}")
    private String botId;
    @Value("${dooray.bot.botToken}")
    private String botToken;

    private static final String X_DEATH_HEADER = "x-death";

    public String getErrorReason(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers.containsKey(X_DEATH_HEADER)) {
            List<Map<String, Object>> deaths = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
            if (!deaths.isEmpty()) {
                return String.valueOf(deaths.get(0).get("reason"));
            }
        }
        return "Unknown";
    }

    public void sendDoorayAlert(String text, String failedMessageBody, String errorReason) {
        try {
            DoorayMessagePayload payload = DoorayMessagePayload.builder()
                    .botName("Coupon-Service-Alarm")
                    .text(text)
                    .attachments(Collections.singletonList(
                            DoorayMessagePayload.Attachment.builder()
                                    .text("실패 원인: " + errorReason + "\n\n" +
                                            "메시지 내용:\n" + failedMessageBody)
                                    .color("red")
                                    .build()
                    ))
                    .build();

            doorayHookClient.sendMessage(serviceId, botId, botToken, payload);
            log.info("Dooray 알림 전송 완료");
        } catch (Exception e) {
            log.error("Dooray 알림 전송 실패", e);
        }
    }
}
