import java.io.IOException;
import java.nio.file.Path;

/** Starts the application. The optional argument selects the data file. */
public class Main {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java Main [path-to-books.csv]");
            System.exit(1);
        }
        Path dataFile = args.length == 1 ? Path.of(args[0]) : Path.of("data", "books.csv");
        BookFileStorage storage = new BookFileStorage(dataFile);
        try {
            BookLibrary library = storage.load();
            new ConsoleUI(library, storage, System.in, System.out).run();
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Cannot open the library: " + ex.getMessage());
            System.err.println("The existing data file has not been changed.");
            System.exit(1);
        }
    }
}
