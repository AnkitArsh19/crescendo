package com.crescendo.apps.readpdf;

import com.crescendo.execution.action.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

@ActionMapping(appKey = "read-pdf", actionKey = "extract-text")
public class ReadPdfExtractTextHandler implements ActionHandler {
    @Override
    public ActionResult execute(ActionContext c) {
        File tmp = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(String.valueOf(c.configuration().getOrDefault("base64", "")));
            tmp = File.createTempFile("crescendo-pdf-", ".pdf");
            Files.write(tmp.toPath(), bytes);
            String pass = String.valueOf(c.configuration().getOrDefault("password", ""));
            try (var doc = pass.isBlank() ? Loader.loadPDF(tmp) : Loader.loadPDF(tmp, pass)) {
                String text = new PDFTextStripper().getText(doc);
                return ActionResult.success(Map.of("text", text, "pages", doc.getNumberOfPages()));
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
