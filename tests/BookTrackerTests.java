import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Dependency-free behavioral tests. Run with test.ps1 or the README commands. */
public class BookTrackerTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        bookRules();
        libraryRules();
        csvRules();
        Path temp = Files.createTempDirectory("book-tracker-tests-");
        try {
            persistence(temp);
            console(temp);
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS: " + checks + " behavioral checks.");
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }

    private static void rejects(Runnable action, String name) {
        try { action.run(); throw new AssertionError("Expected rejection: " + name); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    private static void rejectsIo(IoAction action, String name) throws Exception {
        try { action.run(); throw new AssertionError("Expected IOException: " + name); }
        catch (IOException expected) { checks++; }
    }

    private interface IoAction { void run() throws Exception; }

    private static void bookRules() {
        Book book = new Book(1, " Java ", " Mohammed ", 2026, " Programming ", 100);
        check(book.getTitle().equals("Java") && book.getAuthor().equals("Mohammed"), "trim metadata");
        check(book.getStatus() == ReadingStatus.NOT_STARTED, "initial status");
        rejects(() -> book.setRating(4), "rating an unfinished book");
        rejects(() -> book.setPagesRead(101), "progress above page count");
        rejects(() -> book.setPagesRead(-1), "negative progress");
        book.setPagesRead(50);
        check(book.getStatus() == ReadingStatus.READING && book.getProgressPercent() == 50.0, "reading progress");
        rejects(() -> book.updateDetails("Changed", "Other", 2026, "Other", 49), "invalid edit");
        check(book.getTitle().equals("Java") && book.getTotalPages() == 100, "invalid edit is atomic");
        book.setPagesRead(100);
        book.setRating(5);
        check(book.getStatus() == ReadingStatus.FINISHED && book.getRating() == 5, "finish and rate");
        book.updateDetails("Java", "Mohammed", 2026, "Programming", 150);
        check(book.getStatus() == ReadingStatus.READING && book.getRating() == 0, "more pages clears finished rating");
        book.setPagesRead(150);
        book.setRating(3);
        book.setPagesRead(0);
        check(book.getStatus() == ReadingStatus.NOT_STARTED && book.getRating() == 0, "reset progress clears rating");
        rejects(() -> new Book(0, "T", "A", 2026, "C", 10), "invalid ID");
        rejects(() -> new Book(1, " ", "A", 2026, "C", 10), "blank title");
        rejects(() -> new Book(1, "T", null, 2026, "C", 10), "null author");
        rejects(() -> new Book(1, "T", "A", 0, "C", 10), "invalid year");
        rejects(() -> new Book(1, "T", "A", 2026, "C", 0), "zero pages");
        rejects(() -> book.setRating(6), "invalid rating");
    }

    private static void libraryRules() {
        BookLibrary library = new BookLibrary();
        rejects(() -> library.addBook("", "A", 2026, "C", 10), "invalid add");
        check(library.getNextId() == 1 && library.size() == 0, "failed add preserves ID and library");
        Book a = library.addBook("Zulu Java", "Mohammed", 2024, "Programming", 100);
        Book b = library.addBook("Alpha", "Other", 2025, "Fiction", 50);
        Book c = library.addBook("كتاب", "كاتب", 2026, "تعليم", 20);
        b.setPagesRead(50);
        b.setRating(4);
        a.setPagesRead(25);
        check(library.search("JAVA").size() == 1, "case-insensitive title search");
        check(library.search("moh").get(0) == a, "author search");
        check(library.search("تعليم").get(0) == c, "Unicode category search");
        check(library.search("missing").isEmpty(), "no search results");
        rejects(() -> library.search("  "), "empty query");
        check(library.filter(ReadingStatus.FINISHED).equals(List.of(b)), "status filtering");
        check(library.sorted(1).get(0) == b, "title sorting");
        check(library.sorted(2).get(0) == a, "author sorting");
        check(library.sorted(3).get(0) == c, "year sorting");
        check(library.sorted(4).get(0) == b && library.sorted(5).get(0) == b, "rating and progress sorting");
        check(library.getBooks().get(0) == a, "sorting does not reorder stored books");
        library.getBooks().clear();
        check(library.size() == 3, "list copy protects library membership");
        LibraryStatistics stats = new LibraryStatistics(library.getBooks());
        check(stats.getNotStarted() == 1 && stats.getReading() == 1 && stats.getFinished() == 1, "status statistics");
        check(stats.getPagesRead() == 75 && stats.getTotalPages() == 170, "page statistics");
        check(stats.getAverageRating() == 4.0 && stats.getRated() == 1, "exclude unrated books from average");
        check(new LibraryStatistics(List.of()).getAverageRating() == 0, "empty statistics");
        library.removeBook(c.getId());
        check(library.size() == 2 && library.getNextId() == 4, "delete preserves next ID");
        rejects(() -> library.getBook(99), "unknown ID");
    }

    private static void csvRules() throws Exception {
        List<String> values = List.of("comma,value", "a\"quote", "two\nlines", "العربية", "", " padded ");
        check(CsvCodec.decode(CsvCodec.encodeRow(values)).equals(List.of(values)), "quoted CSV round trip");
        check(CsvCodec.decode("\uFEFFa,b\r\n1,2").size() == 2, "BOM and no final newline");
        check(CsvCodec.decode("a,").get(0).equals(List.of("a", "")), "trailing empty field");
        check(CsvCodec.decode("").isEmpty(), "empty CSV parser");
        rejectsIo(() -> CsvCodec.decode("\"unclosed"), "unclosed quote");
        rejectsIo(() -> CsvCodec.decode("\"a\"bad"), "text after quote");
        rejectsIo(() -> CsvCodec.decode("a\"b"), "quote in unquoted field");
    }

    private static void persistence(Path temp) throws Exception {
        BookFileStorage storage = new BookFileStorage(temp.resolve("books.csv"));
        check(storage.load().size() == 0, "missing file starts empty");
        BookLibrary library = new BookLibrary();
        Book book = library.addBook("كتاب, \"Java\"", "كاتب", 2026, "تعليم", 200);
        book.setPagesRead(200);
        book.setRating(5);
        Book deleted = library.addBook("Deleted", "A", 2026, "C", 10);
        library.removeBook(deleted.getId());
        storage.save(library);
        byte[] firstSave = Files.readAllBytes(storage.getPath());
        BookLibrary loaded = storage.load();
        Book restored = loaded.getBook(1);
        check(restored.getTitle().equals(book.getTitle()) && restored.getAuthor().equals("كاتب"), "UTF-8 metadata persistence");
        check(restored.getPagesRead() == 200 && restored.getRating() == 5, "progress and rating persistence");
        check(restored.getAddedOn().equals(LocalDate.now()), "date persistence");
        check(loaded.addBook("New", "A", 2026, "C", 10).getId() == 3, "deleted highest ID not reused after restart");
        storage.save(loaded);
        check(java.util.Arrays.equals(firstSave, Files.readAllBytes(temp.resolve("books.csv.bak"))), "previous save backup");
        Path corrupt = temp.resolve("corrupt.csv");
        Files.writeString(corrupt, "broken", StandardCharsets.UTF_8);
        rejectsIo(() -> new BookFileStorage(corrupt).load(), "reject malformed file");
        check(Files.readString(corrupt).equals("broken"), "malformed file unchanged");
        String valid = Files.readString(storage.getPath());
        List<List<String>> records = CsvCodec.decode(valid);
        String duplicate = valid + CsvCodec.encodeRow(records.get(2));
        Files.writeString(corrupt, duplicate);
        rejectsIo(() -> new BookFileStorage(corrupt).load(), "reject duplicate IDs");
        records.get(2).set(6, "999");
        StringBuilder badProgress = new StringBuilder();
        for (List<String> record : records) badProgress.append(CsvCodec.encodeRow(record));
        Files.writeString(corrupt, badProgress);
        rejectsIo(() -> new BookFileStorage(corrupt).load(), "reject invalid stored progress");
        Path blocker = temp.resolve("blocker");
        Files.writeString(blocker, "not a folder");
        rejectsIo(() -> new BookFileStorage(blocker.resolve("data.csv")).save(library), "report save failure");
    }

    private static String runConsole(BookLibrary library, BookFileStorage storage, String script) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
        new ConsoleUI(library, storage, new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), out).run();
        return output.toString(StandardCharsets.UTF_8);
    }

    private static void console(Path temp) throws Exception {
        BookFileStorage storage = new BookFileStorage(temp.resolve("console.csv"));
        BookLibrary library = new BookLibrary();
        String script = "wrong\n99\n1\n\nJava Basics\nMohammed\nbad\n2026\nProgramming\n0\n100\n"
                + "6\n1\n101\n100\n7\n1\n6\n5\n4\njava\n9\n3\n10\n4\n11\n"
                + "5\n1\nJava Updated\n\n\n\n\n3\n1\n8\n1\nn\n0\n";
        String result = runConsole(library, storage, script);
        check(result.contains("Enter a whole number") && result.contains("Enter between 1 and 200"), "invalid input retries");
        check(result.contains("Deletion cancelled.") && library.size() == 1, "cancel deletion");
        check(library.getBook(1).getTitle().equals("Java Updated"), "edit keeps blank values");
        check(result.contains("Average rating: 5.00/5"), "console statistics");
        check(storage.load().getBook(1).getRating() == 5, "console autosave");
        runConsole(library, storage, "8\n1\ny\n0\n");
        check(storage.load().size() == 0, "confirmed delete persists");
        BookLibrary interrupted = new BookLibrary();
        runConsole(interrupted, storage, "1\nIncomplete\n");
        check(interrupted.size() == 0, "EOF during add leaves library unchanged");
        String empty = runConsole(new BookLibrary(), storage, "3\n0\n");
        check(empty.contains("library is empty"), "empty library action is handled");
        Path blocked = temp.resolve("ui-blocker");
        Files.writeString(blocked, "block");
        BookLibrary unsaved = new BookLibrary();
        String failed = runConsole(unsaved, new BookFileStorage(blocked.resolve("data.csv")),
                "1\nBook\nAuthor\n2026\nCategory\n10\n0\ny\n");
        check(failed.contains("SAVE FAILED") && failed.contains("lose unsaved changes?"), "save failure requires explicit discard on exit");
    }
}
