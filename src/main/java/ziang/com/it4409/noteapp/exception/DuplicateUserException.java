package ziang.com.it4409.noteapp.exception;

public class DuplicateUserException extends RuntimeException {

    public enum Field {
        USERNAME,
        EMAIL
    }

    private final Field field;

    public DuplicateUserException(Field field) {
        super("Duplicate user field: " + field);
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
