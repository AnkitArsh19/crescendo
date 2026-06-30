package com.crescendo.emailservice.spam;

import com.crescendo.enums.EmailType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailContentSpamCheckerService {

    private static final Pattern IMG_TAG = Pattern.compile("<img[^>]+>");
    private static final Pattern A_TAG = Pattern.compile("<a[^>]+>");
    private static final Pattern SPAM_PHRASES = Pattern.compile("(?i)(100% free|act now|buy now|free money|limited time|guarantee|risk free)");

    public SpamCheckResult checkContent(String subject, String htmlBody, String textBody, EmailType emailType) {
        if (emailType == EmailType.TRANSACTIONAL) {
            return SpamCheckResult.ok();
        }

        List<String> warnings = new ArrayList<>();
        
        boolean hasHtml = htmlBody != null && !htmlBody.isBlank();
        boolean hasText = textBody != null && !textBody.isBlank();

        if (hasHtml && !hasText) {
            warnings.add("Missing plain-text alternative. Emails with only HTML are more likely to be flagged as spam.");
        }

        if (hasHtml) {
            int textLength = htmlBody.replaceAll("<[^>]*>", "").trim().length();
            
            // Image to text ratio
            Matcher imgMatcher = IMG_TAG.matcher(htmlBody);
            int imgCount = 0;
            while (imgMatcher.find()) imgCount++;
            
            if (imgCount > 0 && textLength < 100) {
                warnings.add("Low text-to-image ratio. Emails that are mostly images with very little text are often filtered.");
            }

            // Link density
            Matcher aMatcher = A_TAG.matcher(htmlBody);
            int linkCount = 0;
            while (aMatcher.find()) linkCount++;
            
            if (linkCount > 0 && textLength > 0 && (linkCount * 20 > textLength)) { // Rough heuristic: lot of links for very little text
                warnings.add("High link density. Consider reducing the number of links relative to the text content.");
            }
        }

        String fullText = (subject != null ? subject : "") + " " + (htmlBody != null ? htmlBody : "") + " " + (textBody != null ? textBody : "");
        Matcher spamMatcher = SPAM_PHRASES.matcher(fullText);
        if (spamMatcher.find()) {
            warnings.add("Contains phrasing often associated with spam (e.g., '100% free', 'act now'). Consider revising.");
        }

        return new SpamCheckResult(!warnings.isEmpty(), warnings);
    }
}
