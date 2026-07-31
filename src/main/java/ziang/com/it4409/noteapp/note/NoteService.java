package ziang.com.it4409.noteapp.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziang.com.it4409.noteapp.exception.NoteNotFoundException;
import ziang.com.it4409.noteapp.note.dto.NoteForm;
import ziang.com.it4409.noteapp.user.User;
import ziang.com.it4409.noteapp.user.UserRepository;

@Service
public class NoteService {

    public static final int PAGE_SIZE = 12;

    private static final Sort NOTE_ORDER = Sort.by(
            Sort.Order.desc("pinned"),
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id")
    );

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Note> search(String username, String keyword, NoteCategory category, int page) {
        User owner = requireUser(username);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE, NOTE_ORDER);
        return noteRepository.searchOwnedNotes(owner.getId(), normalizedKeyword, category, pageRequest);
    }

    @Transactional(readOnly = true)
    public Note getOwnedNote(String username, Long noteId) {
        User owner = requireUser(username);
        return noteRepository.findByIdAndUserId(noteId, owner.getId())
                .orElseThrow(NoteNotFoundException::new);
    }

    @Transactional
    public Note create(String username, NoteForm form) {
        User owner = requireUser(username);
        Note note = new Note();
        applyForm(note, form);
        note.setUser(owner);
        note.setPinned(false);
        return noteRepository.save(note);
    }

    @Transactional
    public Note update(String username, Long noteId, NoteForm form) {
        Note note = getOwnedNote(username, noteId);
        applyForm(note, form);
        return noteRepository.save(note);
    }

    @Transactional
    public void delete(String username, Long noteId) {
        Note note = getOwnedNote(username, noteId);
        noteRepository.delete(note);
    }

    @Transactional
    public void togglePinned(String username, Long noteId) {
        Note note = getOwnedNote(username, noteId);
        note.setPinned(!note.isPinned());
        noteRepository.save(note);
    }

    private User requireUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

    private void applyForm(Note note, NoteForm form) {
        note.setTitle(form.getTitle().trim());
        note.setContent(form.getContent().trim());
        note.setCategory(form.getCategory());
    }
}
