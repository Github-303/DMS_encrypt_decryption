package com.document.web.service;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Date;

@Service
public class TextWatermarkService {

    public byte[] createWatermarkedFile(byte[] content, String watermarkText) throws IOException {
        String text = new String(content, StandardCharsets.UTF_8);
        StringBuilder watermarked = new StringBuilder();
        String timestamp = new Date().toString();

        // Add header watermark
        watermarked.append("CONFIDENTIAL DOCUMENT - ").append(timestamp).append("\n");
        watermarked.append("============================================\n\n");

        // Add watermark to each line
        for (String line : text.split("\n")) {
            watermarked.append(line)
                    .append(" [")
                    .append(watermarkText)
                    .append("]\n");
        }

        // Add footer watermark
        watermarked.append("\n============================================\n");
        watermarked.append("Protected document. Do not distribute.\n");

        return watermarked.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String addWatermarkToHtml(String content, String watermarkText) {
        StringBuilder watermarked = new StringBuilder();
        watermarked.append("<!DOCTYPE html><html><head><style>\n");
        watermarked.append(".watermark { color: #grey; opacity: 0.5; position: fixed; }\n");
        watermarked.append("</style></head><body>\n");
        watermarked.append("<div class='watermark'>").append(watermarkText).append("</div>\n");
        watermarked.append(content);
        watermarked.append("</body></html>");
        return watermarked.toString();
    }
}