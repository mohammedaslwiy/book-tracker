import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Reads whole lines so numeric input never leaves an unexpected newline behind. */
public class InputReader {
    public static class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final Scanner scanner;
    private final PrintStream out;

    public InputReader(InputStream in, PrintStream out) {
        scanner = new Scanner(in, StandardCharsets.UTF_8);
        this.out = out;
    }

    private String line(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine().strip();
    }

    public String text(String prompt) {
        while (true) {
            String value = line(prompt);
            if (!value.isBlank() && value.length() <= 200) return value;
            out.println("Enter between 1 and 200 characters.");
        }
    }

    public String textOrKeep(String prompt, String current) {
        while (true) {
            String value = line(prompt);
            if (value.isEmpty()) return current;
            if (value.length() <= 200) return value;
            out.println("Enter no more than 200 characters.");
        }
    }

    public int number(String prompt, int min, int max) {
        while (true) {
            String value = line(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) return number;
            } catch (NumberFormatException ignored) { }
            out.println("Enter a whole number between " + min + " and " + max + ".");
        }
    }

    public int numberOrKeep(String prompt, int current, int min, int max) {
        while (true) {
            String value = line(prompt);
            if (value.isEmpty()) return current;
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) return number;
            } catch (NumberFormatException ignored) { }
            out.println("Enter a whole number between " + min + " and " + max + ", or press Enter to keep.");
        }
    }

    public long bookId() {
        while (true) {
            try {
                long id = Long.parseLong(line("Book ID: "));
                if (id > 0) return id;
            } catch (NumberFormatException ignored) { }
            out.println("Enter a positive book ID.");
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = line(prompt + " [y/n]: ");
            if (answer.equalsIgnoreCase("y")) return true;
            if (answer.equalsIgnoreCase("n")) return false;
            out.println("Enter y or n.");
        }
    }
}
