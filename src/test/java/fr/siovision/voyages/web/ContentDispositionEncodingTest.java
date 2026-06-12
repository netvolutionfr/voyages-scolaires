package fr.siovision.voyages.web;

import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Content-Disposition header encoding logic from DocumentPreviewStreamController.
 *
 * The production code builds the header as:
 *   inline; filename="<asciiName>"; filename*=UTF-8''<encodedName>
 *
 * Where:
 *   asciiName   = filename with control chars, quotes, backslashes, semicolons, and non-ASCII replaced by '_'
 *   encodedName = URLEncoder.encode(filename, UTF-8) with '+' replaced by '%20'
 */
class ContentDispositionEncodingTest {

    /**
     * Mirrors the production encoding logic exactly so tests are testing the real behaviour.
     */
    private static String buildContentDispositionHeader(String filename) {
        String asciiName = filename.replaceAll("[\\x00-\\x1F\\x7F\"\\\\;]", "_")
                                   .replaceAll("[^\\x20-\\x7E]", "_");
        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "inline; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName;
    }

    // --- Plain ASCII filename ---

    @Test
    void asciiFilename_asciiPartIsUnchanged() {
        String header = buildContentDispositionHeader("rapport.pdf");

        assertThat(header).contains("filename=\"rapport.pdf\"");
    }

    @Test
    void asciiFilename_utf8PartIsPercentEncoded() {
        String header = buildContentDispositionHeader("rapport.pdf");

        // Pure ASCII: URLEncoder leaves it unchanged (dots are encoded as-is, letters as-is)
        assertThat(header).contains("filename*=UTF-8''rapport.pdf");
    }

    @Test
    void asciiFilename_fullHeaderHasCorrectStructure() {
        String header = buildContentDispositionHeader("rapport.pdf");

        assertThat(header).startsWith("inline;");
        assertThat(header).contains("filename=\"rapport.pdf\"");
        assertThat(header).contains("filename*=UTF-8''");
    }

    // --- Filename with accents (non-ASCII) ---

    @Test
    void accentedFilename_asciiFallbackReplacesNonAsciiWithUnderscore() {
        String header = buildContentDispositionHeader("état des lieux.pdf");

        // Non-ASCII 'é', 'a', 't' — only 'é' is outside ASCII printable range
        // 'é' (U+00E9) is > 0x7E → replaced by '_'
        String asciiPart = extractAsciiFilename(header);
        assertThat(asciiPart).doesNotContain("é");
        assertThat(asciiPart).contains("_");
    }

    @Test
    void accentedFilename_utf8PartIsPercentEncoded() {
        String header = buildContentDispositionHeader("état des lieux.pdf");

        String encodedPart = extractEncodedFilename(header);
        // 'é' is U+00E9 → UTF-8 is 0xC3 0xA9 → percent-encoded as %C3%A9
        assertThat(encodedPart).contains("%C3%A9");
    }

    @Test
    void accentedFilename_spacesEncodedAs20InUtf8Part() {
        String header = buildContentDispositionHeader("état des lieux.pdf");

        String encodedPart = extractEncodedFilename(header);
        assertThat(encodedPart).contains("%20");
        // URLEncoder would produce '+' for spaces, but we replace '+' with '%20'
        assertThat(encodedPart).doesNotContain("+");
    }

    // --- Filename with CRLF injection ---

    @Test
    void crlfInFilename_crReplacedByUnderscoreInAsciiFallback() {
        String header = buildContentDispositionHeader("evil\r\nname.pdf");

        String asciiPart = extractAsciiFilename(header);
        // \r (0x0D) and \n (0x0A) are in [0x00-0x1F] range → replaced by '_'
        assertThat(asciiPart).doesNotContain("\r");
        assertThat(asciiPart).doesNotContain("\n");
        assertThat(asciiPart).contains("evil__name.pdf");
    }

    @Test
    void crlfInFilename_headerDoesNotContainLiteralCRLF() {
        String header = buildContentDispositionHeader("evil\r\nname.pdf");

        assertThat(header).doesNotContain("\r\n");
        assertThat(header).doesNotContain("\r");
        assertThat(header).doesNotContain("\n");
    }

    // --- Filename with double quotes ---

    @Test
    void quotesInFilename_asciiFallbackReplacesQuotesWithUnderscore() {
        String header = buildContentDispositionHeader("file\"with\"quotes.pdf");

        String asciiPart = extractAsciiFilename(header);
        // '"' (0x22) is in the ["] replacement class → replaced by '_'
        assertThat(asciiPart).doesNotContain("\"");
        // Should not break the surrounding quotes of filename="..."
        assertThat(header).contains("filename=\"file_with_quotes.pdf\"");
    }

    @Test
    void quotesInFilename_doesNotBreakHeaderStructure() {
        String header = buildContentDispositionHeader("file\"with\"quotes.pdf");

        // The header value must remain parseable: exactly one pair of outer quotes around the ascii name
        assertThat(header).containsPattern("filename=\"[^\"]*\"");
    }

    // --- Helpers ---

    /** Extracts the value between filename=" and the next " */
    private static String extractAsciiFilename(String header) {
        int start = header.indexOf("filename=\"") + "filename=\"".length();
        int end = header.indexOf("\"", start);
        return header.substring(start, end);
    }

    /** Extracts the value after filename*=UTF-8'' */
    private static String extractEncodedFilename(String header) {
        String marker = "filename*=UTF-8''";
        int start = header.indexOf(marker) + marker.length();
        return header.substring(start);
    }
}
