package ziang.com.it4409.noteapp.exception;

public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException() {
        super("Note not found");
    }
}
