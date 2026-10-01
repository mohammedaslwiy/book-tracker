import java.util.List;

/** Calculates a snapshot without modifying books. */
public class LibraryStatistics {
    private int notStarted;
    private int reading;
    private int finished;
    private int rated;
    private long totalPages;
    private long pagesRead;
    private long ratingSum;

    public LibraryStatistics(List<Book> books) {
        for (Book book : books) {
            switch (book.getStatus()) {
                case NOT_STARTED: notStarted++; break;
                case READING: reading++; break;
                case FINISHED: finished++; break;
            }
            totalPages += book.getTotalPages();
            pagesRead += book.getPagesRead();
            if (book.getRating() > 0) { rated++; ratingSum += book.getRating(); }
        }
    }

    public int getNotStarted() { return notStarted; }
    public int getReading() { return reading; }
    public int getFinished() { return finished; }
    public int getRated() { return rated; }
    public long getTotalPages() { return totalPages; }
    public long getPagesRead() { return pagesRead; }
    public double getAverageRating() { return rated == 0 ? 0 : (double) ratingSum / rated; }
}
