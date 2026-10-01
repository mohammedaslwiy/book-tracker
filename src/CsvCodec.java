import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** CSV quoting and parsing, including commas, escaped quotes and multiline fields. */
public final class CsvCodec {
    private CsvCodec() { }

    public static String encodeRow(List<String> fields) {
        List<String> quoted = new ArrayList<>();
        for (String field : fields) quoted.add("\"" + field.replace("\"", "\"\"") + "\"");
        return String.join(",", quoted) + "\r\n";
    }

    public static List<List<String>> decode(String text) throws IOException {
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean closedQuote = false;
        boolean fieldStarted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                        closedQuote = true;
                    }
                } else field.append(ch);
            } else if (ch == ',' || ch == '\r' || ch == '\n') {
                row.add(field.toString());
                field.setLength(0);
                closedQuote = false;
                fieldStarted = false;
                if (ch != ',') {
                    rows.add(row);
                    row = new ArrayList<>();
                    if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                }
            } else if (closedQuote) {
                throw new IOException("Unexpected text after a closing CSV quote.");
            } else if (ch == '"') {
                if (fieldStarted) throw new IOException("Unexpected quote inside a CSV field.");
                inQuotes = true;
                fieldStarted = true;
            } else {
                field.append(ch);
                fieldStarted = true;
            }
        }
        if (inQuotes) throw new IOException("Unclosed CSV quote.");
        if (fieldStarted || closedQuote || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
