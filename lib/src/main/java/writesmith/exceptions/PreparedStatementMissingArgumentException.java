package writesmith.exceptions;

public class PreparedStatementMissingArgumentException extends Exception {

    String description;

    public PreparedStatementMissingArgumentException() {
        description = "";
    }

    public PreparedStatementMissingArgumentException(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
