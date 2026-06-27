package com.crescendo.apps.readpdf;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

@Component
public class ReadPdfHandlers {

    @ActionMapping(appKey = "readpdf", actionKey = "extract-text")
    public ActionResult extractText(ActionContext c) {
        File tmp = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(String.valueOf(c.configuration().getOrDefault("base64", "")));
            tmp = File.createTempFile("crescendo-pdf-", ".pdf");
            Files.write(tmp.toPath(), bytes);
            String pass = String.valueOf(c.configuration().getOrDefault("password", ""));
            String textPropertyName = String.valueOf(c.configuration().getOrDefault("textPropertyName", "text"));
            
            try (var doc = pass.isBlank() ? Loader.loadPDF(tmp) : Loader.loadPDF(tmp, pass)) {
                String text = new PDFTextStripper().getText(doc);
                return ActionResult.success(Map.of(textPropertyName, text, "pages", doc.getNumberOfPages()));
            }
        } catch (Exception e) {
            return ActionResult.failure("Read PDF failed: " + e.getMessage());
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }
}
