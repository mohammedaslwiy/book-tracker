import java.time.LocalDate;
import java.time.Year;

/** One book. Validation happens before any field is changed. */
public final class Book {
    private final long id;
    private final LocalDate addedOn;
    private String title;
    private String author;
    private int publicationYear;
    private String category;
    private int totalPages;
    private int pagesRead;
    private int rating;

    public Book(long id, String title, String author, int publicationYear,
                String category, int totalPages) {
        this(id, title, author, publicationYear, category, totalPages, 0, 0, LocalDate.now());
    }

    public Book(long id, String title, String author, int publicationYear,
                String category, int totalPages, int pagesRead, int rating, LocalDate addedOn) {
        if (id < 1 || id == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Book ID is outside the supported range.");
        }
        if (addedOn == null) throw new IllegalArgumentException("Added date is required.");
        updateDetails(title, author, publicationYear, category, totalPages);
        setPagesRead(pagesRead);
        setRating(rating);
        this.id = id;
        this.addedOn = addedOn;
    }

    public void updateDetails(String title, String author, int year, String category, int pages) {
        String checkedTitle = requireText(title, "Title");
        String checkedAuthor = requireText(author, "Author");
        String checkedCategory = requireText(category, "Category");
        if (year < 1 || year > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("Publication year must be between 1 and "
                    + (Year.now().getValue() + 1) + ".");
        }
        if (pages < 1 || pages > 1_000_000) {
            throw new IllegalArgumentException("Total pages must be between 1 and 1000000.");
        }
        if (pages < pagesRead) {
            throw new IllegalArgumentException("Total pages cannot be less than pages already read.");
        }
        this.title = checkedTitle;
        this.author = checkedAuthor;
        this.publicationYear = year;
        this.category = checkedCategory;
        this.totalPages = pages;
        if (pagesRead < totalPages) rating = 0;
    }

    private static String requireText(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        String cleaned = text.strip();
        if (cleaned.length() > 200) {
            throw new IllegalArgumentException(field + " cannot exceed 200 characters.");
        }
        return cleaned;
    }

    public void setPagesRead(int pages) {
        if (pages < 0 || pages > totalPages) {
            throw new IllegalArgumentException("Pages read must be between 0 and " + totalPages + ".");
        }
        pagesRead = pages;
        if (getStatus() != ReadingStatus.FINISHED) rating = 0;
    }

    public void setRating(int rating) {
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("Rating must be between 0 and 5.");
        if (rating > 0 && getStatus() != ReadingStatus.FINISHED) {
            throw new IllegalArgumentException("Finish the book before rating it.");
        }
        this.rating = rating;
    }

    public ReadingStatus getStatus() {
        if (pagesRead == 0) return ReadingStatus.NOT_STARTED;
        if (pagesRead == totalPages) return ReadingStatus.FINISHED;
        return ReadingStatus.READING;
    }

    public double getProgressPercent() { return pagesRead * 100.0 / totalPages; }
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPublicationYear() { return publicationYear; }
    public String getCategory() { return category; }
    public int getTotalPages() { return totalPages; }
    public int getPagesRead() { return pagesRead; }
    public int getRating() { return rating; }
    public LocalDate getAddedOn() { return addedOn; }
}
