package com.crescendo.emailservice.emailtemplate;

import com.crescendo.emailservice.emailtemplate.TemplateVariableValidator;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateVariableValidatorTest {

    private final TemplateVariableValidator validator = new TemplateVariableValidator();

    @Test
    void publish_rejectsUndeclaredReference() {
        EmailTemplate_command template = template("Your order {{ORDER_ID}}", "<p>Ready</p>");

        assertThatThrownBy(() -> validator.validateForPublish(template))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ORDER_ID");
    }

    @Test
    void publish_acceptsDeclaredAndReservedReferences() {
        EmailTemplate_command template = template("Hi {{FIRST_NAME}}", "<p>Order {{ORDER_ID}}</p>");
        template.setVariables(List.of(new EmailTemplate_command.TemplateVariable(
                "ORDER_ID", EmailTemplate_command.VariableType.STRING, null)));

        validator.validateForPublish(template);
    }

    @Test
    void publish_rejectsInvalidOrDuplicateDeclarations() {
        EmailTemplate_command invalid = template("Hi", "<p>Ready</p>");
        invalid.setVariables(List.of(new EmailTemplate_command.TemplateVariable(
                "order-id", EmailTemplate_command.VariableType.STRING, null)));

        assertThatThrownBy(() -> validator.validateForPublish(invalid))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("uppercase letters");

        EmailTemplate_command duplicate = template("Hi", "<p>Ready</p>");
        duplicate.setVariables(List.of(
                new EmailTemplate_command.TemplateVariable("ORDER_ID", EmailTemplate_command.VariableType.STRING, null),
                new EmailTemplate_command.TemplateVariable("ORDER_ID", EmailTemplate_command.VariableType.STRING, "fallback")
        ));

        assertThatThrownBy(() -> validator.validateForPublish(duplicate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("more than once");
    }

    @Test
    void send_rejectsMissingRequiredVariableButAcceptsFallback() {
        EmailTemplate_command template = template("Hi", "<p>Ready</p>");
        template.setVariables(List.of(
                new EmailTemplate_command.TemplateVariable("ORDER_ID", EmailTemplate_command.VariableType.STRING, null),
                new EmailTemplate_command.TemplateVariable("PLAN", EmailTemplate_command.VariableType.STRING, "Free")
        ));

        assertThatThrownBy(() -> validator.validateForSend(template, Map.of("PLAN", "Pro")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ORDER_ID");

        validator.validateForSend(template, Map.of("ORDER_ID", "ORD-42", "PLAN", "Pro"));
    }

    private EmailTemplate_command template(String subject, String htmlBody) {
        return new EmailTemplate_command(UUID.randomUUID(), UUID.randomUUID(), "Order update", subject, htmlBody, null);
    }
}
