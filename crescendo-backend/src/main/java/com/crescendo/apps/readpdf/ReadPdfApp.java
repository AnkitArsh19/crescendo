package com.crescendo.apps.readpdf;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ReadPdfApp implements AppDefinition {
    public App toApp() {
        return new App(
                "readpdf",
                "Read PDF", """
                The Read PDF app is a built-in utility that allows you to parse and extract text content from PDF documents, enabling you to process invoices, resumes, and reports automatically.

                **What you can do with Read PDF in Crescendo:**
                - Extract text from a candidate's resume uploaded via Typeform and send it to Gemini for a summary
                - Parse incoming PDF invoices from an IMAP mailbox and extract the total amount owed
                - Read daily PDF reports from a legacy FTP server and convert the text into structured JSON data

                **Actions available:**
                - Extract Text — input Base64-encoded PDF data (and an optional password) to extract all text content

                **Who should use this:** HR professionals, finance teams, and developers automating document ingestion.

                **Authentication:** None required.
                """,
                "/icons/pdf.svg",
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "extract-text",
                                "name", "Extract Text",
                                "description", "Read PDF and extract text",
                                "configSchema", List.of(
                                        Map.of("key", "base64", "label", "PDF Base64", "type", "textarea", "required", true),
                                        Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                                        Map.of("key", "textPropertyName", "label", "Text Property Name", "type", "text", "required", true, "placeholder", "text", "helpText", "Name of the property to which to write the extracted text.")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
