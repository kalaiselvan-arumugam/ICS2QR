package com.mymeetings.qr.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

public class Minifier {

    public static String minifyAndEncode(String rawIcs) throws IOException {
        String minifiedIcs = minifyIcs(rawIcs);
        return compressAndEncode(minifiedIcs);
    }

    public static String minifyIcs(String rawIcs) {
        // Step 1: Unfold lines
        String unfolded = rawIcs
                .replace("\r\n ", "")
                .replace("\r\n\t", "")
                .replace("\n ", "")
                .replace("\n\t", "");

        String[] lines = unfolded.split("\r?\n");

        String uid = "";
        String summary = "";
        String dtstart = "";
        String dtend = "";
        String rrule = "";
        List<String> exdates = new ArrayList<>();
        String location = "";
        String description = "";

        boolean inEvent = false;
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.equalsIgnoreCase("BEGIN:VEVENT")) {
                inEvent = true;
                continue;
            }
            if (cleanLine.equalsIgnoreCase("END:VEVENT")) {
                break;
            }
            if (!inEvent) continue;

            int colonIdx = cleanLine.indexOf(':');
            if (colonIdx == -1) continue;
            String keyParams = cleanLine.substring(0, colonIdx);
            String value = cleanLine.substring(colonIdx + 1);

            String[] paramParts = keyParams.split(";");
            String key = paramParts[0].toUpperCase();

            switch (key) {
                case "UID":
                    uid = cleanLine;
                    break;
                case "SUMMARY":
                    summary = cleanLine;
                    break;
                case "DTSTART":
                    dtstart = cleanLine;
                    break;
                case "DTEND":
                    dtend = cleanLine;
                    break;
                case "RRULE":
                    rrule = cleanLine;
                    break;
                case "EXDATE":
                    exdates.add(cleanLine);
                    break;
                case "LOCATION":
                    location = cleanLine;
                    break;
                case "DESCRIPTION":
                    description = value;
                    break;
            }
        }

        // Reconstruct only the critical elements
        StringBuilder builder = new StringBuilder();
        builder.append("BEGIN:VCALENDAR\n");
        builder.append("VERSION:2.0\n");
        builder.append("BEGIN:VEVENT\n");

        if (!uid.isEmpty()) builder.append(uid).append("\n");
        if (!summary.isEmpty()) builder.append(summary).append("\n");
        if (!dtstart.isEmpty()) builder.append(dtstart).append("\n");
        if (!dtend.isEmpty()) builder.append(dtend).append("\n");
        if (!rrule.isEmpty()) builder.append(rrule).append("\n");
        for (String ex : exdates) {
            builder.append(ex).append("\n");
        }
        if (!location.isEmpty()) builder.append(location).append("\n");

        // Compress Description to only contain the extracted Teams join details
        String teamsUrl = extractTeamsUrl(description);
        if (teamsUrl == null && location.contains("teams.microsoft.com")) {
            teamsUrl = extractTeamsUrl(location);
        }
        String[] credentials = extractTeamsCredentials(description);

        if (teamsUrl != null || credentials != null) {
            StringBuilder descBuilder = new StringBuilder();
            if (credentials != null) {
                if (credentials[0] != null) descBuilder.append("Meeting ID: ").append(credentials[0]).append("\\n");
                if (credentials[1] != null) descBuilder.append("Passcode: ").append(credentials[1]).append("\\n");
            }
            if (teamsUrl != null) {
                descBuilder.append("Join Link: ").append(teamsUrl);
            }
            builder.append("DESCRIPTION:").append(descBuilder.toString()).append("\n");
        }

        builder.append("END:VEVENT\n");
        builder.append("END:VCALENDAR");

        return builder.toString();
    }

    private static String compressAndEncode(String plainText) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(plainText.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    private static String extractTeamsUrl(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("https?://[a-zA-Z0-9.-]*teams\\.microsoft\\.com/l/meetup-join/\\S+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String url = matcher.group();
            // Clean up trailing characters
            if (url.endsWith(">")) url = url.substring(0, url.length() - 1);
            if (url.endsWith(")")) url = url.substring(0, url.length() - 1);
            return url;
        }
        return null;
    }

    private static String[] extractTeamsCredentials(String text) {
        if (text == null) return null;
        Pattern idPattern = Pattern.compile("Meeting\\s*ID:?\\s*([0-9\\s\\-]+)", Pattern.CASE_INSENSITIVE);
        Pattern passPattern = Pattern.compile("Passcode:?\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE);

        Matcher idMatcher = idPattern.matcher(text);
        String idVal = null;
        if (idMatcher.find()) {
            idVal = idMatcher.group(1).replaceAll("[\\s\\-]", "").trim();
        }

        Matcher passMatcher = passPattern.matcher(text);
        String passVal = null;
        if (passMatcher.find()) {
            passVal = passMatcher.group(1).trim();
        }

        if (idVal != null && idVal.length() >= 9) {
            return new String[]{idVal, passVal};
        }
        return null;
    }
}
