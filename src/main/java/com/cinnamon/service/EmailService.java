package com.cinnamon.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:wello3606@gmail.com}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        boolean forRegister = "REGISTER".equalsIgnoreCase(purpose);
        String badgeTitle = forRegister ? "XÁC THỰC ĐĂNG KÝ" : "XÁC THỰC ĐĂNG NHẬP";
        String mainHeading = forRegister ? "Chào mừng bạn đến với Cinnamon!" : "Yêu cầu đăng nhập tài khoản";
        String actionDesc = forRegister
                ? "Cảm ơn bạn đã lựa chọn <b>Cinnamon</b> để đồng hành trên lộ trình học tiếng Anh. Dưới đây là mã OTP để hoàn tất tạo tài khoản của bạn:"
                : "Hệ thống vừa nhận được yêu cầu đăng nhập tài khoản <b>Cinnamon</b> của bạn. Nhập mã OTP bên dưới để tiếp tục:";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Cinnamon English");
            helper.setTo(toEmail);
            helper.setSubject("🌿 [" + otp + "] " + (forRegister ? "Mã xác thực đăng ký" : "Mã đăng nhập") + " Cinnamon");

            String htmlContent = buildOtpEmailHtml(badgeTitle, mainHeading, actionDesc, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email OTP thành công tới: {}", toEmail);

        } catch (Exception ex) {
            log.error("Không thể gửi email qua SMTP: {}. Mã OTP dự phòng: [{}] gửi tới: {}", ex.getMessage(), otp, toEmail, ex);
            System.out.println("=================================================");
            System.out.println("  [CINNAMON EMAIL OTP] EMAIL: " + toEmail);
            System.out.println("  [CINNAMON EMAIL OTP] MÃ OTP: " + otp);
            System.out.println("=================================================");
            throw new RuntimeException("Không thể gửi email xác thực lúc này: " + ex.getMessage(), ex);
        }
    }

    private String buildOtpEmailHtml(String badgeTitle, String mainHeading, String actionDesc, String otp) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Cinnamon OTP</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f1f5f9; padding: 40px 15px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 540px; background-color: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 12px 35px rgba(120, 53, 15, 0.08); border: 1px solid #e2e8f0;">
                                <!-- HEADER GRADIENT -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #78350f 0%%, #b45309 50%%, #d97706 100%%); padding: 40px 30px 32px 30px; text-align: center;">
                                        <div style="display: inline-block; background: rgba(255, 255, 255, 0.2); backdrop-filter: blur(10px); padding: 10px 22px; border-radius: 50px; border: 1px solid rgba(255, 255, 255, 0.3); margin-bottom: 12px;">
                                            <span style="font-size: 26px; line-height: 1; vertical-align: middle;">🌿</span>
                                            <span style="color: #ffffff; font-size: 22px; font-weight: 800; letter-spacing: 1.5px; vertical-align: middle; margin-left: 8px;">CINNAMON</span>
                                        </div>
                                        <p style="margin: 0; color: #fef3c7; font-size: 12px; font-weight: 700; letter-spacing: 2.5px; text-transform: uppercase;">English Learning Platform</p>
                                    </td>
                                </tr>
                                
                                <!-- MAIN BODY -->
                                <tr>
                                    <td style="padding: 38px 36px 30px 36px; text-align: center;">
                                        <!-- BADGE -->
                                        <div style="display: inline-block; padding: 6px 16px; background: #fef3c7; color: #92400e; border-radius: 20px; font-size: 11px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 16px; border: 1px solid #fde68a;">
                                            %s
                                        </div>
                                        
                                        <h2 style="margin: 0 0 14px 0; color: #0f172a; font-size: 23px; font-weight: 800; line-height: 1.35;">
                                            %s
                                        </h2>
                                        
                                        <p style="margin: 0 0 28px 0; color: #475569; font-size: 14.5px; line-height: 1.65;">
                                            %s
                                        </p>
                                        
                                        <!-- OTP HIGHLIGHT CARD -->
                                        <div style="background: linear-gradient(180deg, #fffbeb 0%%, #fef3c7 100%%); border: 2px dashed #f59e0b; border-radius: 18px; padding: 22px 24px; margin: 0 auto 28px auto; max-width: 330px; box-shadow: inset 0 2px 4px rgba(245, 158, 11, 0.08);">
                                            <div style="color: #92400e; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 8px;">Mã xác thực của bạn</div>
                                            <div style="color: #b45309; font-size: 38px; font-weight: 900; letter-spacing: 9px; font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace; padding-left: 9px; text-shadow: 0 1px 2px rgba(180, 83, 9, 0.2);">
                                                %s
                                            </div>
                                        </div>
                                        
                                        <!-- SECURITY & TIMER NOTICE -->
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f8fafc; border-radius: 14px; padding: 14px 18px; border: 1px solid #e2e8f0; margin-bottom: 24px; text-align: left;">
                                            <tr>
                                                <td style="width: 32px; vertical-align: top; font-size: 20px; line-height: 1.2;">⏱️</td>
                                                <td style="color: #334155; font-size: 13px; line-height: 1.55;">
                                                    Mã có hiệu lực trong <b>5 phút</b>. Tuyệt đối <b>không chia sẻ</b> mã này cho bất kỳ ai (kể cả nhân viên Cinnamon) để đảm bảo an toàn tài khoản.
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="margin: 0; color: #94a3b8; font-size: 12.5px; line-height: 1.55;">
                                            Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email. Tài khoản của bạn vẫn được an toàn.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- FOOTER -->
                                <tr>
                                    <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px 30px; text-align: center;">
                                        <p style="margin: 0 0 6px 0; color: #64748b; font-size: 12px; font-weight: 600;">
                                            🌿 Cinnamon • Học tiếng Anh thông minh & Cá nhân hóa
                                        </p>
                                        <p style="margin: 0; color: #94a3b8; font-size: 11px;">
                                            © 2026 Cinnamon Platform. Đây là email tự động, vui lòng không phản hồi.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(badgeTitle, mainHeading, actionDesc, otp);
    }
}
