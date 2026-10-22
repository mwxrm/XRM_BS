package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.service.CaptchaService;
import com.xrm.xrm_bs.service.VerificationCodeStore;
import com.xrm.xrm_bs.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final VerificationCodeStore codeStore;
    private final EmailService emailService;

    @GetMapping("/math")
    public ResponseEntity<Map<String, String>> getMathCaptcha() {
        CaptchaService.SimpleMathCaptcha captcha = captchaService.generateMathCaptcha();
        String token = codeStore.generateAndStoreMathResult(captcha.result);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "image", captcha.base64Image
        ));
    }

    @PostMapping("/send-email")
    public ResponseEntity<String> sendEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String mathToken = request.get("mathToken");
        int mathAnswer = Integer.parseInt(request.get("mathAnswer"));

        if (!codeStore.verifyMath(mathToken, mathAnswer)) {
            return ResponseEntity.badRequest().body("人机验证失败，请刷新后重试");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        codeStore.storeEmailCode(email, code);
        emailService.sendVerificationCode(email, code);
        return ResponseEntity.ok("验证码已发送");
    }
}