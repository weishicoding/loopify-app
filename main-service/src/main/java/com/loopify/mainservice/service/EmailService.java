package com.loopify.mainservice.service;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${app.mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${app.mailgun.domain}")
    private String mailgunDomain;

    @Value("${app.mailgun.from-email}")
    private String fromEmail;

    @Value("${app.mailgun.isSendEmail}")
    private boolean sendEmailsEnabled;

    public void sendVerificationCode(String toEmail, String code) {
        // Simple flag for local dev - can be enhanced with profiles
        // Set to false for local testing without sending
        if (!sendEmailsEnabled) {
            // todo : 测试无法发送邮件，暂时显示code
            log.warn("Email sending disabled. Verification code for {}: {}", toEmail, code);
            return; // Skip sending in local dev if disabled
        }

        Client client = ClientBuilder.newClient();
        client.register(HttpAuthenticationFeature.basic("api", mailgunApiKey));

        WebTarget target = client.target("https://api.mailgun.net/v3")
                .path(mailgunDomain)
                .path("messages");

        Form form = new Form();
        form.param("from", fromEmail);
        form.param("to", toEmail);
        form.param("subject", "Your Verification Code");
        form.param("text", "Your verification code is: " + code + "\nIt expires in 5 minutes.");

        log.info("Sending verification code email to {}", toEmail);
        try {
            Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED)
                    .post(Entity.form(form));

            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                log.info("Verification email sent successfully to {}. Status: {}", toEmail, response.getStatus());
            } else {
                String responseBody = response.readEntity(String.class);
                log.error("Failed to send verification email to {}. Status: {}, Response: {}",
                        toEmail, response.getStatus(), responseBody);
                // Handle failure appropriately - maybe throw exception
            }
            response.close();
        } catch (Exception e) {
            log.error("Exception while sending verification email to {}: {}", toEmail, e.getMessage(), e);
            // Handle failure
        } finally {
            client.close();
        }
    }
}
