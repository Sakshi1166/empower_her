package com.empowerher.services;

import com.empowerher.entities.Scheme;
import com.empowerher.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    // UserRepository directly use karen instead of UserService
    @Autowired
    private com.empowerher.repositories.UserRepository userRepository;

    @Value("${app.email.from:empowerher.noreply@gmail.com}")
    private String fromEmail;

    @Value("${app.email.admin:admin@empowerher.com}")
    private String adminEmail;

    @Value("${spring.mail.username}")
    private String systemEmail;

    @Value("${app.public-url:http://localhost:8081}")
    private String publicUrl;

    // Send Welcome Email to New User
    @Async
    public void sendWelcomeEmail(User user) {
        try {
            System.out.println("Sending welcome email to: " + user.getEmail());
            
            Context context = new Context();
            context.setVariable("name", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("loginUrl", publicUrl + "/login");
            context.setVariable("websiteUrl", publicUrl);
            context.setVariable("supportEmail", "support@empowerher.com");

            String htmlContent = templateEngine.process("emails/welcome-email", context);

            sendEmail(user.getEmail(), 
                     "Welcome to EmpowerHer - Start Your Empowerment Journey!", 
                     htmlContent);
            
            System.out.println("Welcome email sent successfully to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send New Scheme Notification to All Users
    @Async
    public void sendNewSchemeNotification(Scheme scheme) {
        try {
            System.out.println("Sending new scheme notification for: " + scheme.getTitle());
            
            // UserRepository directly use karen
            List<User> activeUsers = userRepository.findAll().stream()
                    .filter(User::isEnabled)
                    .toList();

            System.out.println("Sending to " + activeUsers.size() + " users");

            for (User user : activeUsers) {
                try {
                    sendNewSchemeEmailToUser(user, scheme);
                    // Small delay to avoid overwhelming email service
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Failed to send to " + user.getEmail() + ": " + e.getMessage());
                }
            }
            
            System.out.println("New scheme notifications sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send new scheme notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send New Scheme Email to Individual User
    private void sendNewSchemeEmailToUser(User user, Scheme scheme) {
        try {
            Context context = new Context();
            context.setVariable("userName", user.getUsername());
            context.setVariable("schemeTitle", scheme.getTitle());
            context.setVariable("schemeShortDesc", scheme.getShortDesc());
            context.setVariable("schemeLevel", scheme.getLevel());
            context.setVariable("schemeCategory", scheme.getCategory().getName());
            context.setVariable("applyLink", scheme.getApplyLink());
            context.setVariable("schemeUrl", publicUrl + "/scheme/" + scheme.getId());
            context.setVariable("websiteUrl", publicUrl);
            context.setVariable("unsubscribeUrl", publicUrl + "/user/profile");

            String htmlContent = templateEngine.process("emails/new-scheme-notification", context);

            sendEmail(user.getEmail(), 
                     "🎯 New Scheme: " + scheme.getTitle() + " - EmpowerHer", 
                     htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send scheme email to " + user.getEmail() + ": " + e.getMessage());
        }
    }

    // Send Admin Notification for New User Registration
    @Async
    public void sendAdminNewUserNotification(User user) {
        try {
            Context context = new Context();
            context.setVariable("userName", user.getUsername());
            context.setVariable("userEmail", user.getEmail());
            context.setVariable("registrationDate", user.getCreatedAt());
            context.setVariable("adminUrl", publicUrl + "/admin/users");

            String htmlContent = templateEngine.process("emails/admin-new-user", context);

            sendEmail(adminEmail, 
                     "👤 New User Registration - EmpowerHer", 
                     htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send admin notification: " + e.getMessage());
        }
    }

    // Generic Email Sending Method
    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true indicates HTML

        // Add email headers for better deliverability
        message.addHeader("List-Unsubscribe", "<mailto:" + systemEmail + "?subject=Unsubscribe>");
        message.addHeader("X-Priority", "3"); // Normal priority

        mailSender.send(message);
    }

    // Test Email Method
    @Async
    public void sendTestEmail(String toEmail) {
        try {
            Context context = new Context();
            context.setVariable("message", "This is a test email from EmpowerHer system.");
            context.setVariable("timestamp", java.time.LocalDateTime.now());

            String htmlContent = templateEngine.process("emails/test-email", context);

            sendEmail(toEmail, "✅ EmpowerHer - Test Email", htmlContent);
            
            System.out.println("Test email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send test email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}