package com.pms.reservation.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public class LenientLocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mm a"),
            DateTimeFormatter.ofPattern("hh:mm a")
    );

    @Override
    public LocalTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return parseFromString(parser, parser.getValueAsString());
        }

        if (token == JsonToken.START_OBJECT) {
            JsonNode node = parser.readValueAsTree();
            LocalTime fromText = parseObjectTextField(parser, node);
            if (fromText != null) {
                return fromText;
            }

            Integer hour = readInt(node, "hour", "hours", "hh", "h");
            Integer minute = readInt(node, "minute", "minutes", "mm", "m");
            Integer second = readInt(node, "second", "seconds", "ss", "s");
            if (hour != null && minute != null) {
                int resolvedSecond = second == null ? 0 : second;
                try {
                    return LocalTime.of(hour, minute, resolvedSecond);
                } catch (RuntimeException ex) {
                    throw invalid(parser, "Invalid time object values");
                }
            }

            throw invalid(parser, "Time object must contain text value or hour/minute fields");
        }

        return (LocalTime) context.handleUnexpectedToken(LocalTime.class, parser);
    }

    private LocalTime parseObjectTextField(JsonParser parser, JsonNode node) throws IOException {
        String[] textKeys = {"value", "time", "label", "text"};
        for (String key : textKeys) {
            JsonNode textNode = node.get(key);
            if (textNode != null && textNode.isTextual()) {
                String raw = textNode.asText();
                if (StringUtils.hasText(raw)) {
                    return parseFromString(parser, raw);
                }
            }
        }
        return null;
    }

    private Integer readInt(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode valueNode = node.get(key);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }

            if (valueNode.isInt() || valueNode.isLong()) {
                return valueNode.intValue();
            }

            if (valueNode.isTextual()) {
                String raw = valueNode.asText();
                if (!StringUtils.hasText(raw)) {
                    continue;
                }
                try {
                    return Integer.parseInt(raw.trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
        }
        return null;
    }

    private LocalTime parseFromString(JsonParser parser, String raw) throws IOException {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String candidate = raw.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalTime.parse(candidate.toUpperCase(Locale.ROOT), formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        throw invalid(parser, "Unsupported time format: " + raw);
    }

    private InvalidFormatException invalid(JsonParser parser, String detail) {
        return InvalidFormatException.from(
                parser,
                detail + ". Supported values: HH:mm[:ss], h:mm a, or object with hour/minute",
            null,
                LocalTime.class
        );
    }
}
