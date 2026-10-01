import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Validates a complete file before loading; replaces saved data via a temporary file. */
public class BookFileStorage {
    private static final List<String> HEADER = List.of("id", "title", "author", "year", "category",
            "totalPages", "pagesRead", "rating", "addedOn");
    private final Path path;

    public BookFileStorage(Path path) { this.path = path.toAbsolutePath().normalize(); }
    public Path getPath() { return path; }

    public BookLibrary load() throws IOException {
        BookLibrary library = new BookLibrary();
        if (!Files.exists(path)) return library;
        List<List<String>> rows = CsvCodec.decode(Files.readString(path, StandardCharsets.UTF_8));
        if (rows.size() < 2 || rows.get(0).size() != 3
                || !rows.get(0).get(0).equals("BOOK_TRACKER") || !rows.get(0).get(1).equals("1")
                || !rows.get(1).equals(HEADER)) {
            throw new IOException("Invalid or unsupported library file: " + path);
        }
        for (int i = 2; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() != HEADER.size()) throw new IOException("Invalid field count in record " + (i + 1));
            try {
                Book book = new Book(Long.parseLong(row.get(0)), row.get(1), row.get(2),
                        Integer.parseInt(row.get(3)), row.get(4), Integer.parseInt(row.get(5)),
                        Integer.parseInt(row.get(6)), Integer.parseInt(row.get(7)), LocalDate.parse(row.get(8)));
                library.restore(book);
            } catch (IllegalArgumentException | DateTimeParseException ex) {
                throw new IOException("Invalid book in record " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        try {
            library.restoreNextId(Long.parseLong(rows.get(0).get(2)));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid next ID: " + ex.getMessage(), ex);
        }
        return library;
    }

    public void save(BookLibrary library) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append(CsvCodec.encodeRow(List.of("BOOK_TRACKER", "1", Long.toString(library.getNextId()))));
        csv.append(CsvCodec.encodeRow(HEADER));
        for (Book book : library.getBooks()) {
            csv.append(CsvCodec.encodeRow(List.of(Long.toString(book.getId()), book.getTitle(),
                    book.getAuthor(), Integer.toString(book.getPublicationYear()), book.getCategory(),
                    Integer.toString(book.getTotalPages()), Integer.toString(book.getPagesRead()),
                    Integer.toString(book.getRating()), book.getAddedOn().toString())));
        }
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), ".book-tracker-", ".tmp");
        try {
            Files.writeString(temp, csv, StandardCharsets.UTF_8);
            if (Files.exists(path)) {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
