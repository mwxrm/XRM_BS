package com.xrm.xrm_bs.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

@Service
public class CaptchaService {

    private final Random random = new Random();

    public static class SimpleMathCaptcha {
        public String expression;
        public int result;
        public String base64Image;
    }

    public SimpleMathCaptcha generateMathCaptcha() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        int op = random.nextInt(3); // 0(+), 1(-), 2(*)
        String opStr;
        int result;
        switch (op) {
            case 0: opStr = "+"; result = a + b; break;
            case 1: opStr = "-"; result = a - b; break;
            default: opStr = "×"; result = a * b; break;
        }
        String expression = a + " " + opStr + " " + b + " = ?";
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 120, 40);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(expression, 10, 30);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException("验证码生成失败");
        }
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        SimpleMathCaptcha captcha = new SimpleMathCaptcha();
        captcha.expression = expression;
        captcha.result = result;
        captcha.base64Image = base64;
        return captcha;
    }
}