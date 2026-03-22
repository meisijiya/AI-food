package com.ai.food.service.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String email, String code) {
        log.info("发送验证码 - email: {}, code: {}", email, code);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            helper.setSubject("AI Food 验证码");
            String html = "<html><body>"
                    + "<h3>AI Food 注册验证码</h3>"
                    + "<p>您的验证码为: <strong style='font-size:24px;'>" + code + "</strong></p>"
                    + "<p>验证码有效期为5分钟，请勿泄露给他人。</p>"
                    + "</body></html>";
            helper.setText(html, true);
            mailSender.send(message);
            log.info("验证码邮件发送成功 - email: {}", email);
        } catch (MessagingException e) {
            log.error("验证码邮件发送失败 - email: {}", email, e);
            throw new RuntimeException("邮件发送失败");
        }
    }
}
