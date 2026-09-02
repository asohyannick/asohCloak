package com.asohCloak.asohCloak.service.resendMailService;

import com.asohCloak.asohCloak.config.resendConfig.resendProperties.ResendProperties;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResendMailService {

    private static final Logger log = LoggerFactory.getLogger(ResendMailService.class);

    private final Resend resendClient;
    private final ResendProperties resendProperties;

    /**
     * Sends a single HTML email via Resend. Runs synchronously on the calling
     * thread, so callers that don't want to block the request thread should
     * invoke this from inside AsyncTaskRunner.runInBackground(...).
     */
    public CreateEmailResponse sendEmail(String to, String subject, String htmlBody) {
        String from = resendProperties.getSender().getName()
                + " <" + resendProperties.getSender().getEmail() + ">";

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();

        try {
            CreateEmailResponse response = resendClient.emails().send(params);
            log.info("Email sent to {} via Resend, id={}", to, response.getId());
            return response;
        } catch (ResendException e) {
            log.error("Failed to send email to {} via Resend: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via Resend", e);
        }
    }
}