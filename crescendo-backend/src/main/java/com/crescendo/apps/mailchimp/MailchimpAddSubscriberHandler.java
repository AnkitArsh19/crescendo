package com.crescendo.apps.mailchimp;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.security.MessageDigest;
import java.util.*;
import java.util.HexFormat;

@ActionMapping(appKey = "mailchimp", actionKey = "add-subscriber")
public class MailchimpAddSubscriberHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public MailchimpAddSubscriberHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String email = SimpleApiSupport.cfg(c, "email").toLowerCase(Locale.ROOT);
            if (email.isBlank()) {
                return ActionResult.failure("Mailchimp email is required");
            }
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(email.getBytes()));
            String base = "https://" + SimpleApiSupport.cred(c, "serverPrefix") + ".api.mailchimp.com/3.0";
            String res = SimpleApiSupport.basic(base, "anystring", SimpleApiSupport.cred(c, "apiKey"))
                    .put()
                    .uri("/lists/{listId}/members/{hash}", SimpleApiSupport.cfg(c, "listId"), hash)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email_address", email,
                            "status_if_new", SimpleApiSupport.cfg(c, "status").isBlank() ? "subscribed" : SimpleApiSupport.cfg(c, "status")
                    ))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Mailchimp add subscriber failed: " + e.getMessage());
        }
    }
}
