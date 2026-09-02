package com.asohCloak.asohCloak.config.emailTemplateMessager;

import java.time.Year;

/**
 * Centralized builder for all outbound HTML emails sent by AsohClock.
 * Every method returns a complete, self-contained HTML document (inline CSS)
 * suitable for passing directly to the Resend API as the "html" field.
 *
 * Design system:
 *  - 600px centered card layout (standard, safe width for all email clients)
 *  - Inline styles only (Gmail/Outlook strip <style> blocks in <body>)
 *  - table-based structure for maximum client compatibility (Outlook/Word engine)
 *  - Consistent brand header + footer via {@link #wrapEmailShell}
 */
public class EmailTemplateMessager {

    // ---------------------------------------------------------------------
    // Brand tokens
    // ---------------------------------------------------------------------
    private static final String BRAND_NAME    = "AsohClock";
    private static final String PRIMARY       = "#4F46E5"; // indigo
    private static final String PRIMARY_DARK  = "#3730A3";
    private static final String TEXT_DARK     = "#1F2937";
    private static final String TEXT_MUTED    = "#6B7280";
    private static final String BORDER_COLOR  = "#E5E7EB";
    private static final String SUCCESS       = "#059669";
    private static final String DANGER        = "#DC2626";
    private static final String WARNING       = "#D97706";

    // =======================================================================
    // 1. WELCOME + EMAIL VERIFICATION (OTP)
    // =======================================================================
    public static String sendWelcomeVerificationEmailAsync(String firstName, String lastName, String otpCode) {
        String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Welcome to AsohClock, {{FIRST_NAME}} 👋
            </h1>
            <p style="margin:0 0 24px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                We're excited to have you on board. To activate your account, please confirm it's really you by entering the verification code below in the app.
            </p>
            {{OTP_BOX}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                This code expires in <strong style="color:{{TEXT_DARK}};">10 minutes</strong>.
            </p>
            {{INFO_BOX}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:1.6;color:{{TEXT_MUTED}};">
                If you didn't create an AsohClock account, you can safely ignore this email.
            </p>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{OTP_BOX}}", otpBox(otpCode))
                .replace("{{INFO_BOX}}", infoBox("For your security, never share this code with anyone &mdash; AsohClock staff will never ask for it.", WARNING))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED);

        return wrapEmailShell(
                "Verify your email - AsohClock",
                "Your AsohClock verification code is " + otpCode,
                body
        );
    }

    // =======================================================================
    // 2. RESEND OTP CODE (previous code expired)
    // =======================================================================
    public static String resendOTPCodeAsync(String firstName, String lastName, String otpCode) {
        String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Here's your new code, {{FIRST_NAME}}
            </h1>
            <p style="margin:0 0 24px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                Your previous verification code expired, so we've generated a fresh one for you. Enter the code below to continue.
            </p>
            {{OTP_BOX}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                This code expires in <strong style="color:{{TEXT_DARK}};">10 minutes</strong>.
            </p>
            {{INFO_BOX}}
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{OTP_BOX}}", otpBox(otpCode))
                .replace("{{INFO_BOX}}", infoBox("Didn't request a new code? You can ignore this email &mdash; your account remains secure.", WARNING))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED);

        return wrapEmailShell(
                "Your new verification code - AsohClock",
                "Your new AsohClock verification code is " + otpCode,
                body
        );
    }

    // =======================================================================
    // 3. OTP VERIFIED SUCCESSFULLY
    // =======================================================================
    public static String verifyOtpCodeAsync(String firstName, String lastName) {
        String body = """
            <div style="text-align:center;margin:0 0 24px 0;">
                <div style="display:inline-block;width:64px;height:64px;line-height:64px;border-radius:50%;background-color:#ECFDF5;color:{{SUCCESS}};font-size:32px;font-weight:700;font-family:Arial,Helvetica,sans-serif;">&#10003;</div>
            </div>
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};text-align:center;">
                Email Verified, {{FIRST_NAME}}!
            </h1>
            <p style="margin:0 0 28px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                Your email address has been successfully verified and your AsohClock account is now fully active. You're all set to get started.
            </p>
            <div style="text-align:center;">
                {{CTA_BUTTON}}
            </div>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED)
                .replace("{{SUCCESS}}", SUCCESS);

        return wrapEmailShell(
                "Email verified - AsohClock",
                "Your email has been successfully verified.",
                body
        );
    }

    // =======================================================================
    // 4. FORGOT PASSWORD REQUEST
    // =======================================================================
    public static String forgotPasswordEmailAsync(String firstName, String lastName, String resetLink) {
        String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Reset your password
            </h1>
            <p style="margin:0 0 24px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                Hi {{FIRST_NAME}}, we received a request to reset the password for your AsohClock account. Click the button below to choose a new password.
            </p>
            <div style="text-align:center;margin:8px 0 8px 0;">
                {{CTA_BUTTON}}
            </div>
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                This link will expire in <strong style="color:{{TEXT_DARK}};">30 minutes</strong>.
            </p>
            {{INFO_BOX}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:1.6;color:{{TEXT_MUTED}};">
                If you didn't request a password reset, no action is needed &mdash; your password will remain unchanged.
            </p>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{CTA_BUTTON}}", ctaButton(resetLink, "Reset Password", PRIMARY))
                .replace("{{INFO_BOX}}", infoBox("For your security, this link can only be used once.", WARNING))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED);

        return wrapEmailShell(
                "Reset your password - AsohClock",
                "Reset your AsohClock account password.",
                body
        );
    }

    // =======================================================================
    // 5. PASSWORD RESET SUCCESSFUL
    // =======================================================================
    public static String resetPasswordEmailAsync(String firstName, String lastName, String loginUrl) {
        String body = """
            <div style="text-align:center;margin:0 0 24px 0;">
                <div style="display:inline-block;width:64px;height:64px;line-height:64px;border-radius:50%;background-color:#ECFDF5;color:{{SUCCESS}};font-size:32px;font-weight:700;font-family:Arial,Helvetica,sans-serif;">&#10003;</div>
            </div>
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};text-align:center;">
                Password Changed Successfully
            </h1>
            <p style="margin:0 0 28px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                Hi {{FIRST_NAME}}, this confirms that the password for your AsohClock account was just changed. You can now log in using your new password.
            </p>
            <div style="text-align:center;margin:0 0 8px 0;">
                {{CTA_BUTTON}}
            </div>
            {{INFO_BOX}}
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{CTA_BUTTON}}", ctaButton(loginUrl, "Log In", PRIMARY))
                .replace("{{INFO_BOX}}", infoBox("If you didn't make this change, please contact our support team immediately to secure your account.", DANGER))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED)
                .replace("{{SUCCESS}}", SUCCESS);

        return wrapEmailShell(
                "Password changed - AsohClock",
                "Your AsohClock account password was changed successfully.",
                body
        );
    }

    // =======================================================================
    // 10. COURSE MEDIA READY (background upload finished, all files succeeded)
    // =======================================================================
    public static String courseMediaReadyEmailAsync(String firstName, String lastName, String courseName, String courseUrl) {
        String body = """
        <div style="text-align:center;margin:0 0 24px 0;">
            <div style="display:inline-block;width:64px;height:64px;line-height:64px;border-radius:50%;background-color:#ECFDF5;color:{{SUCCESS}};font-size:32px;font-weight:700;font-family:Arial,Helvetica,sans-serif;">&#10003;</div>
        </div>
        <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};text-align:center;">
            Your course media is ready
        </h1>
        <p style="margin:0 0 28px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
            Hi {{FIRST_NAME}}, all videos and documents for <strong style="color:{{TEXT_DARK}};">{{COURSE_NAME}}</strong> have finished uploading and are now live for students.
        </p>
        <div style="text-align:center;">
            {{CTA_BUTTON}}
        </div>
        """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{COURSE_NAME}}", courseName)
                .replace("{{CTA_BUTTON}}", ctaButton(courseUrl, "View Course", PRIMARY))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED)
                .replace("{{SUCCESS}}", SUCCESS);

        return wrapEmailShell(
                "Course media ready - AsohClock",
                "All media for \"" + courseName + "\" has finished uploading.",
                body
        );
    }

    // =======================================================================
    // 11. COURSE MEDIA UPLOAD ISSUE (background upload finished, some/all failed)
    // =======================================================================
        public static String courseMediaPartiallyFailedEmailAsync(String firstName, String lastName, String courseName,
                                                                  String courseUrl, java.util.List<String> failedFileNames) {
            String fileListHtml = failedFileNames.stream()
                    .map(name -> "<li style=\"margin:0 0 4px 0;\">" + name + "</li>")
                    .reduce("", String::concat);

            String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Some course media couldn't be uploaded
            </h1>
            <p style="margin:0 0 20px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                Hi {{FIRST_NAME}}, your course <strong style="color:{{TEXT_DARK}};">{{COURSE_NAME}}</strong> was created successfully, but the following files failed to upload:
            </p>
            {{INFO_BOX}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:{{TEXT_MUTED}};">
                You can try re-uploading these files from your course dashboard.
            </p>
            <div style="text-align:center;margin-top:20px;">
                {{CTA_BUTTON}}
            </div>
            """
                    .replace("{{FIRST_NAME}}", firstName)
                    .replace("{{COURSE_NAME}}", courseName)
                    .replace("{{INFO_BOX}}", infoBox("<ul style=\"margin:0;padding-left:18px;\">" + fileListHtml + "</ul>", WARNING))
                    .replace("{{CTA_BUTTON}}", ctaButton(courseUrl, "Go to Course", PRIMARY))
                    .replace("{{TEXT_DARK}}", TEXT_DARK)
                    .replace("{{TEXT_MUTED}}", TEXT_MUTED);

            return wrapEmailShell(
                    "Course media upload issue - AsohClock",
                    "Some files for \"" + courseName + "\" failed to upload.",
                    body
            );
        }

    // =======================================================================
    // 6. ACCOUNT BLOCKED BY ADMIN
    // =======================================================================
    public static String blockAccountEmailAsync(String firstName, String lastName, String reason) {
        String reasonText = (reason == null || reason.isBlank())
                ? "a violation of our platform's terms of service."
                : reason;

        String body = """
            <div style="text-align:center;margin:0 0 24px 0;">
                <div style="display:inline-block;width:64px;height:64px;line-height:64px;border-radius:50%;background-color:#FEF2F2;color:{{DANGER}};font-size:30px;font-weight:700;font-family:Arial,Helvetica,sans-serif;">&#33;</div>
            </div>
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};text-align:center;">
                Your Account Has Been Blocked
            </h1>
            <p style="margin:0 0 20px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                Hi {{FIRST_NAME}} {{LAST_NAME}}, your AsohClock account has been blocked by an administrator due to:
            </p>
            {{INFO_BOX}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:{{TEXT_MUTED}};">
                While your account is blocked, you will not be able to log in or access AsohClock services. If you believe this was a mistake, please contact our support team to appeal this decision.
            </p>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{LAST_NAME}}", lastName)
                .replace("{{INFO_BOX}}", infoBox("<strong>Reason:</strong> " + reasonText, DANGER))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED)
                .replace("{{DANGER}}", DANGER);

        return wrapEmailShell(
                "Account blocked - AsohClock",
                "Your AsohClock account has been blocked.",
                body
        );
    }

    // =======================================================================
    // 7. ACCOUNT UNBLOCKED
    // =======================================================================
    public static String unblockAccountEmailAsync(String firstName, String lastName, String loginUrl) {
        String body = """
            <div style="text-align:center;margin:0 0 24px 0;">
                <div style="display:inline-block;width:64px;height:64px;line-height:64px;border-radius:50%;background-color:#ECFDF5;color:{{SUCCESS}};font-size:32px;font-weight:700;font-family:Arial,Helvetica,sans-serif;">&#10003;</div>
            </div>
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};text-align:center;">
                Your Account Has Been Restored
            </h1>
            <p style="margin:0 0 28px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                Good news, {{FIRST_NAME}}! Your AsohClock account has been unblocked and full access has been restored. You can log back in right away.
            </p>
            <div style="text-align:center;">
                {{CTA_BUTTON}}
            </div>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{CTA_BUTTON}}", ctaButton(loginUrl, "Log In to AsohClock", SUCCESS))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED)
                .replace("{{SUCCESS}}", SUCCESS);

        return wrapEmailShell(
                "Account restored - AsohClock",
                "Your AsohClock account has been unblocked.",
                body
        );
    }

    // =======================================================================
    // 8. MAGIC LINK (PASSWORDLESS LOGIN)
    // =======================================================================
    public static String sentMagicLinkEmailAsync(String firstName, String lastName, String magicLink) {
        String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Your login link is ready
            </h1>
            <p style="margin:0 0 24px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                Hi {{FIRST_NAME}}, click the button below to securely sign in to AsohClock &mdash; no password needed.
            </p>
            <div style="text-align:center;margin:8px 0 8px 0;">
                {{CTA_BUTTON}}
            </div>
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:1.6;color:{{TEXT_MUTED}};text-align:center;">
                This link expires in <strong style="color:{{TEXT_DARK}};">15 minutes</strong> and can only be used once.
            </p>
            {{INFO_BOX}}
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{CTA_BUTTON}}", ctaButton(magicLink, "Sign In to AsohClock", PRIMARY))
                .replace("{{INFO_BOX}}", infoBox("If you didn't request this link, you can safely ignore this email &mdash; no one can access your account without clicking it.", WARNING))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED);

        return wrapEmailShell(
                "Your sign-in link - AsohClock",
                "Click to securely sign in to your AsohClock account.",
                body
        );
    }

    // =======================================================================
    // 9. COURSE CREATED CONFIRMATION (TO TUTOR)
    // =======================================================================
    public static String sentCourseEmailAsync(String firstName, String lastName, String courseName, String courseUrl) {
        String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Your course is live! 🎉
            </h1>
            <p style="margin:0 0 20px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                Hi {{FIRST_NAME}} {{LAST_NAME}}, congratulations! Your course has been successfully created on AsohClock and is now visible to students.
            </p>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
                <tr>
                    <td style="background-color:#F9FAFB;border:1px solid {{BORDER_COLOR}};border-radius:8px;padding:20px;">
                        <p style="margin:0 0 4px 0;font-family:Arial,Helvetica,sans-serif;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;color:{{PRIMARY}};">Course</p>
                        <p style="margin:0;font-family:Arial,Helvetica,sans-serif;font-size:17px;font-weight:700;color:{{TEXT_DARK}};">{{COURSE_NAME}}</p>
                    </td>
                </tr>
            </table>
            <p style="margin:0 0 24px 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:{{TEXT_MUTED}};">
                You can manage your course content, track enrollments, and view student progress from your tutor dashboard.
            </p>
            <div style="text-align:center;">
                {{CTA_BUTTON}}
            </div>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{LAST_NAME}}", lastName)
                .replace("{{COURSE_NAME}}", courseName)
                .replace("{{CTA_BUTTON}}", ctaButton(courseUrl, "View Course", PRIMARY))
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED)
                .replace("{{PRIMARY}}", PRIMARY)
                .replace("{{BORDER_COLOR}}", BORDER_COLOR);

        return wrapEmailShell(
                "Course created - AsohClock",
                "Your course \"" + courseName + "\" is now live on AsohClock.",
                body
        );
    }

    // =======================================================================
    // X. ACCOUNT DELETED (self-service deletion confirmation)
    // =======================================================================
    public static String accountDeletedEmailAsync(String firstName, String lastName, String reason) {
        String reasonParagraph = (reason == null || reason.isBlank())
                ? ""
                : """
              <p style="margin:16px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:{{TEXT_MUTED}};">
                  Reason provided: <span style="color:{{TEXT_DARK}};">{{REASON}}</span>
              </p>
              """
                .replace("{{REASON}}", reason)
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED);

        String body = """
            <h1 style="margin:0 0 12px 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:{{TEXT_DARK}};">
                Your account has been deleted, {{FIRST_NAME}}
            </h1>
            <p style="margin:0 0 24px 0;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:{{TEXT_MUTED}};">
                This confirms that your AsohClock account was deleted at your request. You've been signed out everywhere, and any active sessions have been revoked.
            </p>
            {{INFO_BOX}}
            {{REASON_PARAGRAPH}}
            <p style="margin:24px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:1.6;color:{{TEXT_MUTED}};">
                If you didn't request this, please contact support immediately &mdash; someone else may have access to your account.
            </p>
            """
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{INFO_BOX}}", infoBox("This action is permanent and cannot be undone from within the app.", DANGER))
                .replace("{{REASON_PARAGRAPH}}", reasonParagraph)
                .replace("{{TEXT_DARK}}", TEXT_DARK)
                .replace("{{TEXT_MUTED}}", TEXT_MUTED);

        return wrapEmailShell(
                "Your account has been deleted - AsohClock",
                "Your AsohClock account has been deleted",
                body
        );
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /** Wraps inner body HTML in the shared branded header/footer email shell. */
    private static String wrapEmailShell(String title, String preheaderText, String bodyContent) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <meta http-equiv="X-UA-Compatible" content="IE=edge" />
            <title>{{TITLE}}</title>
            <!--[if mso]>
            <noscript>
            <xml>
            <o:OfficeDocumentSettings>
            <o:PixelsPerInch>96</o:PixelsPerInch>
            </o:OfficeDocumentSettings>
            </xml>
            </noscript>
            <![endif]-->
            <style>
                body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
                table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
                img { -ms-interpolation-mode: bicubic; border: 0; height: auto; line-height: 100%; outline: none; text-decoration: none; }
                body { margin: 0; padding: 0; width: 100% !important; background-color: #F3F4F6; }
                a { color: inherit; }
                @media only screen and (max-width: 620px) {
                    .email-container { width: 100% !important; }
                    .email-padding { padding-left: 24px !important; padding-right: 24px !important; }
                }
            </style>
            </head>
            <body style="margin:0;padding:0;background-color:#F3F4F6;">
            <div style="display:none;max-height:0;overflow:hidden;font-size:1px;line-height:1px;color:#F3F4F6;">
                {{PREHEADER}}&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;
            </div>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F3F4F6;">
                <tr>
                    <td align="center" style="padding:40px 16px;">
                        <table role="presentation" class="email-container" width="600" cellpadding="0" cellspacing="0" style="width:600px;max-width:600px;background-color:#FFFFFF;border-radius:12px;overflow:hidden;">
                            <tr>
                                <td style="background:{{PRIMARY}};background:linear-gradient(135deg,{{PRIMARY}} 0%,{{PRIMARY_DARK}} 100%);padding:32px 40px;text-align:center;">
                                    <span style="font-family:Arial,Helvetica,sans-serif;font-size:24px;font-weight:800;color:#FFFFFF;letter-spacing:0.5px;">AsohClock</span>
                                </td>
                            </tr>
                            <tr>
                                <td class="email-padding" style="padding:40px;">
                                    {{BODY}}
                                </td>
                            </tr>
                            <tr>
                                <td style="background-color:#F9FAFB;padding:28px 40px;border-top:1px solid {{BORDER_COLOR}};">
                                    <p style="margin:0 0 8px 0;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.6;color:#9CA3AF;text-align:center;">
                                        This is an automated message from AsohClock. Please do not reply directly to this email.
                                    </p>
                                    <p style="margin:0;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.6;color:#9CA3AF;text-align:center;">
                                        &copy; {{YEAR}} AsohClock. All rights reserved.
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
            </body>
            </html>
            """
                .replace("{{TITLE}}", title)
                .replace("{{PREHEADER}}", preheaderText)
                .replace("{{BODY}}", bodyContent)
                .replace("{{PRIMARY}}", PRIMARY)
                .replace("{{PRIMARY_DARK}}", PRIMARY_DARK)
                .replace("{{BORDER_COLOR}}", BORDER_COLOR)
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    /** Renders an OTP code as individually boxed characters. */
    private static String otpBox(String otpCode) {
        StringBuilder cells = new StringBuilder();
        char[] digits = otpCode.toCharArray();
        for (int i = 0; i < digits.length; i++) {
            cells.append("<td style=\"width:44px;height:52px;text-align:center;vertical-align:middle;")
                    .append("background-color:#F9FAFB;border:2px solid ").append(PRIMARY).append(";border-radius:8px;")
                    .append("font-family:'Courier New',Courier,monospace;font-size:24px;font-weight:700;color:").append(TEXT_DARK)
                    .append(";\">").append(digits[i]).append("</td>");
            if (i < digits.length - 1) {
                cells.append("<td style=\"width:8px;\">&nbsp;</td>");
            }
        }
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\"><tr>"
                + cells
                + "</tr></table>";
    }

    /** Renders a solid, rounded call-to-action button. */
    private static String ctaButton(String url, String label, String backgroundColor) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\">"
                + "<tr><td style=\"border-radius:8px;background-color:" + backgroundColor + ";\">"
                + "<a href=\"" + url + "\" target=\"_blank\" "
                + "style=\"display:inline-block;padding:14px 36px;font-family:Arial,Helvetica,sans-serif;"
                + "font-size:15px;font-weight:700;color:#FFFFFF;text-decoration:none;border-radius:8px;\">"
                + label + "</a></td></tr></table>";
    }

    /** Renders a left-accented information / warning / danger callout box. */
    private static String infoBox(String messageHtml, String accentColor) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:20px 0 0 0;\">"
                + "<tr><td style=\"background-color:#F9FAFB;border-left:4px solid " + accentColor + ";"
                + "border-radius:4px;padding:16px 20px;font-family:Arial,Helvetica,sans-serif;font-size:13.5px;"
                + "line-height:1.6;color:" + TEXT_DARK + ";\">" + messageHtml + "</td></tr></table>";
    }
}