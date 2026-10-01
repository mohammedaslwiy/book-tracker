import java.util.Scanner;

public class FirstBookDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter book title: ");
        String title = scanner.nextLine();
        String author = " MohammedS";
        int year = 2026;

        System.out.println("Title: " + title);
        System.out.println("Author: " + author);
        System.out.println("Year: " + year);
        scanner.close();
    }
}

