package com.crescendo.apps.brandfetch;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BrandfetchApp implements AppDefinition {
    public App toApp() {
        return new App(
                "brandfetch",
                "Brandfetch", """
                Brandfetch is the global brand registry. The Crescendo Brandfetch app allows you to retrieve accurate, up-to-date brand assets directly into your workflows.

                **What you can do with Brandfetch in Crescendo:**
                - Fetch a company's primary logo when a new Salesforce account is created
                - Retrieve brand colors to dynamically style PDF reports or invoices
                - Pull social media links to enrich new CRM leads
                - Validate the existence of a corporate domain automatically

                **Actions available:**
                - Get Brand — provide a domain name (e.g., openai.com) to retrieve a full brand profile including logos, colors, fonts, and descriptions

                **Who should use this:** Marketing automation specialists, designers, and developers building dynamic company profiles.

                **Authentication:** None required for public endpoints (API Key available for higher limits).
                """,
                "https://www.google.com/s2/favicons?domain=brandfetch.com&sz=128",
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "get-brand",
                                "name", "Get Brand",
                                "description", "Fetch brand profile by domain",
                                "configSchema", List.of(
                                        Map.of("key", "domain", "label", "Domain", "type", "text", "required", true, "placeholder", "openai.com")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data").helpUrl("https://docs.brandfetch.com/");
    }
}
