package com.example.guangzhouorder.dto;

import com.example.guangzhouorder.entity.ProductCard;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ProductCardDto {

    private final Long productCardId;
    private final String sku;
    private final String name;
    private final String material;
    private final String configuration;
    private final String stock;
    private final String imageUrl;
    private final String notes;
    private final List<SpecEntry> specs;
    private final BigDecimal displayPrice;
    private final String categoryName;
    private final boolean isPublic;

    public ProductCardDto(ProductCard card) {
        this.productCardId = card.getProductCardId();
        this.sku = "GZ-" + card.getProductCardId();
        this.displayPrice = card.getDisplayPrice();
        this.isPublic = card.isPublic();
        this.categoryName = card.getCategory() != null ? card.getCategory().getName() : "—";

        String dna = card.getCardDna();
        this.name = extract(dna, "name", "Product #GZ-" + card.getProductCardId());
        this.material = extract(dna, "material", "—");
        this.configuration = extract(dna, "configuration", "—");
        this.stock = extract(dna, "stock", "—");
        this.imageUrl = extract(dna, "imageUrl", "");
        this.notes = extract(dna, "notes", "");
        this.specs = parseSpecs(dna);
    }

    /**
     * Extracts a string value for a given key from a flat JSON string.
     * Handles both string values ("key":"value") and number values ("key":123).
     */
    private static String extract(String json, String key, String defaultValue) {
        if (json == null || json.isBlank()) return defaultValue;
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return defaultValue;
        int colon = json.indexOf(':', keyIdx + search.length());
        if (colon < 0) return defaultValue;
        int valueStart = colon + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
        if (valueStart >= json.length()) return defaultValue;
        if (json.charAt(valueStart) == '"') {
            int end = json.indexOf('"', valueStart + 1);
            return end > valueStart ? json.substring(valueStart + 1, end) : defaultValue;
        } else {
            // numeric or boolean value — read until , or } or ]
            int end = valueStart;
            while (end < json.length() && ",}]\n".indexOf(json.charAt(end)) < 0) end++;
            String val = json.substring(valueStart, end).trim();
            return val.isEmpty() ? defaultValue : val;
        }
    }

    /**
     * Parses the "specs" array: [{"label":"...","value":"..."}, ...]
     */
    private static List<SpecEntry> parseSpecs(String json) {
        List<SpecEntry> result = new ArrayList<>();
        if (json == null || !json.contains("\"specs\"")) return result;
        int arrStart = json.indexOf('[', json.indexOf("\"specs\""));
        int arrEnd = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return result;
        String arr = json.substring(arrStart + 1, arrEnd);
        int pos = 0;
        while (pos < arr.length()) {
            int objStart = arr.indexOf('{', pos);
            int objEnd = arr.indexOf('}', objStart);
            if (objStart < 0 || objEnd < 0) break;
            String obj = arr.substring(objStart + 1, objEnd);
            String label = extract("{" + obj + "}", "label", "");
            String value = extract("{" + obj + "}", "value", "");
            if (!label.isEmpty()) result.add(new SpecEntry(label, value));
            pos = objEnd + 1;
        }
        return result;
    }

    @Getter
    public static class SpecEntry {
        private final String label;
        private final String value;

        public SpecEntry(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
