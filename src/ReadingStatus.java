public enum ReadingStatus {
    NOT_STARTED("Not started"), READING("Reading"), FINISHED("Finished");
    private final String label;

    ReadingStatus(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
