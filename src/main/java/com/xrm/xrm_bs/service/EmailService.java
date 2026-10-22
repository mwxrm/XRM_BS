package com.xrm.xrm_bs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String from;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Async("emailExecutor")
    public void sendVerificationCode(String to, String code) {
        if (mailSender == null) {
//            log.info("===== 验证码（开发环境）===== 邮箱：{} 验证码：{}", to, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);   // 必须与认证用户相同
        message.setTo(to);
        message.setSubject("文字社区注册验证码");
        message.setText("您的验证码是：" + code + "，5分钟内有效。");
        try {
            mailSender.send(message);
            log.info("验证码已发送至 {}", to);
        } catch (Exception e) {
            log.error("邮件发送失败，验证码：{}，原因：{}", code, e.getMessage());
        }
    }
}