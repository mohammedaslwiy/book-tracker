import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Manages books and stable IDs; contains no console or file operations. */
public class BookLibrary {
    private final List<Book> books = new ArrayList<>();
    private long nextId = 1;

    public Book addBook(String title, String author, int year, String category, int pages) {
        if (nextId == Long.MAX_VALUE) throw new IllegalArgumentException("No more book IDs are available.");
        Book book = new Book(nextId, title, author, year, category, pages);
        books.add(book);
        nextId++;
        return book;
    }

    public Book getBook(long id) {
        for (Book book : books) if (book.getId() == id) return book;
        throw new IllegalArgumentException("No book found with ID " + id + ".");
    }

    public void removeBook(long id) { books.remove(getBook(id)); }
    public List<Book> getBooks() { return new ArrayList<>(books); }
    public int size() { return books.size(); }
    public long getNextId() { return nextId; }

    public List<Book> search(String query) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Search text cannot be empty.");
        String needle = query.strip().toLowerCase(Locale.ROOT);
        List<Book> found = new ArrayList<>();
        for (Book book : books) {
            if (book.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                    || book.getAuthor().toLowerCase(Locale.ROOT).contains(needle)
                    || book.getCategory().toLowerCase(Locale.ROOT).contains(needle)) found.add(book);
        }
        return found;
    }

    public List<Book> filter(ReadingStatus status) {
        List<Book> found = new ArrayList<>();
        for (Book book : books) if (book.getStatus() == status) found.add(book);
        return found;
    }

    public List<Book> sorted(int choice) {
        List<Book> sorted = getBooks();
        Comparator<Book> comparator;
        switch (choice) {
            case 1: comparator = Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER); break;
            case 2: comparator = Comparator.comparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER); break;
            case 3: comparator = Comparator.comparingInt(Book::getPublicationYear).reversed(); break;
            case 4: comparator = Comparator.comparingInt(Book::getRating).reversed(); break;
            case 5: comparator = Comparator.comparingDouble(Book::getProgressPercent).reversed(); break;
            default: throw new IllegalArgumentException("Unknown sort option.");
        }
        sorted.sort(comparator.thenComparingLong(Book::getId));
        return sorted;
    }

    // Used only when restoring validated records from storage.
    void restore(Book book) {
        for (Book existing : books) {
            if (existing.getId() == book.getId()) throw new IllegalArgumentException("Duplicate book ID: " + book.getId());
        }
        books.add(book);
        nextId = Math.max(nextId, book.getId() + 1);
    }

    void restoreNextId(long savedNextId) {
        if (savedNextId < nextId || savedNextId < 1) throw new IllegalArgumentException("Invalid next book ID in data file.");
        nextId = savedNextId;
    }
}
