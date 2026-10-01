import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Year;
import java.util.List;
import java.util.Locale;

/** Menu and presentation; delegates business rules and storage to other classes. */
public class ConsoleUI {
    private final BookLibrary library;
    private final BookFileStorage storage;
    private final InputReader input;
    private final PrintStream out;
    private boolean unsaved;

    public ConsoleUI(BookLibrary library, BookFileStorage storage, InputStream in, PrintStream out) {
        this.library = library;
        this.storage = storage;
        this.input = new InputReader(in, out);
        this.out = out;
    }

    public void run() {
        out.println("BOOK TRACKER - Personal Reading Library");
        out.println("Loaded " + library.size() + " book(s).");
        out.println("Data file: " + storage.getPath());
        try {
            boolean running = true;
            while (running) {
                menu();
                int choice = input.number("Choose: ", 0, 11);
                try {
                    switch (choice) {
                        case 1: add(); break;
                        case 2: showList(library.getBooks()); break;
                        case 3: details(selectBook()); break;
                        case 4: showList(library.search(input.text("Search title, author or category: "))); break;
                        case 5: edit(); break;
                        case 6: progress(); break;
                        case 7: rate(); break;
                        case 8: remove(); break;
                        case 9: filter(); break;
                        case 10: sort(); break;
                        case 11: statistics(); break;
                        case 0: running = !finish(); break;
                    }
                } catch (IllegalArgumentException ex) {
                    out.println("Cannot complete this action: " + ex.getMessage());
                }
            }
        } catch (InputReader.EndOfInput ex) {
            out.println("\nInput ended.");
            if (unsaved && !save()) out.println("WARNING: changes could not be saved before input ended.");
        }
        out.println("Goodbye!");
    }

    private void menu() {
        out.println("\n1  Add book                 2  List all books");
        out.println("3  Book details             4  Search");
        out.println("5  Edit book                6  Update reading progress");
        out.println("7  Rate a finished book     8  Delete book");
        out.println("9  Filter by reading status 10 Sort books");
        out.println("11 Reading statistics       0  Save and exit");
        if (unsaved) out.println("WARNING: there are unsaved changes.");
    }

    private Book selectBook() {
        if (library.size() == 0) throw new IllegalArgumentException("Your library is empty. Add a book first.");
        return library.getBook(input.bookId());
    }

    private void add() {
        String title = input.text("Title: ");
        String author = input.text("Author: ");
        int year = input.number("Publication year: ", 1, Year.now().getValue() + 1);
        String category = input.text("Category: ");
        int pages = input.number("Total pages: ", 1, 1_000_000);
        Book book = library.addBook(title, author, year, category, pages);
        out.println("Added book #" + book.getId() + ".");
        changed();
    }

    private void edit() {
        Book book = selectBook();
        details(book);
        out.println("Press Enter to keep an existing value.");
        String title = input.textOrKeep("Title: ", book.getTitle());
        String author = input.textOrKeep("Author: ", book.getAuthor());
        int year = input.numberOrKeep("Publication year: ", book.getPublicationYear(), 1, Year.now().getValue() + 1);
        String category = input.textOrKeep("Category: ", book.getCategory());
        int pages = input.numberOrKeep("Total pages: ", book.getTotalPages(), Math.max(1, book.getPagesRead()), 1_000_000);
        book.updateDetails(title, author, year, category, pages);
        out.println("Book updated. Rating is cleared if the book is no longer finished.");
        changed();
    }

    private void progress() {
        Book book = selectBook();
        int pages = input.number("Pages read (total so far): ", 0, book.getTotalPages());
        book.setPagesRead(pages);
        out.println("Reading status: " + book.getStatus());
        changed();
    }

    private void rate() {
        Book book = selectBook();
        if (book.getStatus() != ReadingStatus.FINISHED) throw new IllegalArgumentException("Finish the book before rating it.");
        book.setRating(input.number("Rating 1-5 (0 removes rating): ", 0, 5));
        out.println("Rating updated.");
        changed();
    }

    private void remove() {
        Book book = selectBook();
        details(book);
        if (input.confirm("Delete this book?")) {
            library.removeBook(book.getId());
            out.println("Book deleted.");
            changed();
        } else out.println("Deletion cancelled.");
    }

    private void filter() {
        out.println("1 Not started   2 Reading   3 Finished");
        int choice = input.number("Status: ", 1, 3);
        showList(library.filter(ReadingStatus.values()[choice - 1]));
    }

    private void sort() {
        out.println("1 Title A-Z   2 Author A-Z   3 Year newest first");
        out.println("4 Rating highest first   5 Progress highest first");
        showList(library.sorted(input.number("Sort by: ", 1, 5)));
    }

    private void showList(List<Book> books) {
        if (books.isEmpty()) { out.println("No books to display."); return; }
        out.printf("%-6s %-28s %-22s %-5s %-12s %-13s %-7s%n", "ID", "Title", "Author", "Year", "Pages", "Status", "Rating");
        out.println("------------------------------------------------------------------------------------------------------");
        for (Book book : books) {
            out.printf("%-6d %-28s %-22s %-5d %-12s %-13s %-7s%n", book.getId(), shortText(book.getTitle(), 28),
                    shortText(book.getAuthor(), 22), book.getPublicationYear(), book.getPagesRead() + "/" + book.getTotalPages(),
                    book.getStatus(), book.getRating() == 0 ? "-" : book.getRating() + "/5");
        }
        out.println(books.size() + " book(s). Use book details for the full title and category.");
    }

    private String shortText(String text, int width) {
        String line = text.replaceAll("\\p{Cntrl}", " ");
        return line.length() > width ? line.substring(0, width - 3) + "..." : line;
    }

    private void details(Book book) {
        out.println("\nBook #" + book.getId());
        out.println("Title: " + shortText(book.getTitle(), 203));
        out.println("Author: " + shortText(book.getAuthor(), 203));
        out.println("Publication year: " + book.getPublicationYear());
        out.println("Category: " + shortText(book.getCategory(), 203));
        out.printf(Locale.ROOT, "Reading: %d/%d pages (%.1f%%) - %s%n", book.getPagesRead(), book.getTotalPages(),
                book.getProgressPercent(), book.getStatus());
        out.println("Rating: " + (book.getRating() == 0 ? "Not rated" : book.getRating() + "/5"));
        out.println("Added on: " + book.getAddedOn());
    }

    private void statistics() {
        LibraryStatistics stats = new LibraryStatistics(library.getBooks());
        out.println("\nBooks: " + library.size());
        out.println("Not started: " + stats.getNotStarted() + " | Reading: " + stats.getReading() + " | Finished: " + stats.getFinished());
        out.println("Pages read: " + stats.getPagesRead() + "/" + stats.getTotalPages());
        if (stats.getRated() == 0) out.println("Average rating: no ratings yet.");
        else out.printf(Locale.ROOT, "Average rating: %.2f/5 (%d rated books)%n", stats.getAverageRating(), stats.getRated());
    }

    private void changed() { unsaved = true; save(); }

    private boolean save() {
        try {
            storage.save(library);
            unsaved = false;
            out.println("Library saved.");
            return true;
        } catch (IOException ex) {
            out.println("SAVE FAILED: " + ex.getMessage());
            out.println("Your changes are in memory. Check the data folder and retry saving on exit.");
            return false;
        }
    }

    private boolean finish() {
        if (!unsaved || save()) return true;
        return input.confirm("Saving failed. Exit and lose unsaved changes?");
    }
}
