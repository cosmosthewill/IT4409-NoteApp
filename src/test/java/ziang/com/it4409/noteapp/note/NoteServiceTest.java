package ziang.com.it4409.noteapp.note;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ziang.com.it4409.noteapp.exception.NoteNotFoundException;
import ziang.com.it4409.noteapp.note.dto.NoteForm;
import ziang.com.it4409.noteapp.user.User;
import ziang.com.it4409.noteapp.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    private NoteService noteService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, userRepository);
        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("user-a");
        when(userRepository.findByUsernameIgnoreCase("user-a")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void userCannotViewAnotherUsersNote() {
        when(noteRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getOwnedNote("user-a", 99L))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void userCannotEditAnotherUsersNote() {
        when(noteRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.update("user-a", 99L, validForm()))
                .isInstanceOf(NoteNotFoundException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    void userCannotDeleteAnotherUsersNote() {
        when(noteRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.delete("user-a", 99L))
                .isInstanceOf(NoteNotFoundException.class);

        verify(noteRepository, never()).delete(any());
    }

    @Test
    void searchIsAlwaysScopedToCurrentUserAndCategory() {
        when(noteRepository.searchOwnedNotes(eq(10L), eq("spring"), eq(NoteCategory.STUDY), any(Pageable.class)))
                .thenReturn(Page.empty());

        noteService.search("user-a", "  spring  ", NoteCategory.STUDY, 0);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(noteRepository).searchOwnedNotes(
                eq(10L),
                eq("spring"),
                eq(NoteCategory.STUDY),
                pageable.capture()
        );
        assertThat(pageable.getValue().getPageSize()).isEqualTo(NoteService.PAGE_SIZE);
        assertThat(pageable.getValue().getSort().getOrderFor("pinned").isDescending()).isTrue();
    }

    @Test
    void createAssignsAuthenticatedUserAsOwner() {
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note saved = noteService.create("user-a", validForm());

        assertThat(saved.getUser()).isSameAs(currentUser);
        assertThat(saved.isPinned()).isFalse();
        assertThat(saved.getCategory()).isEqualTo(NoteCategory.STUDY);
    }

    private NoteForm validForm() {
        NoteForm form = new NoteForm();
        form.setTitle("Spring Boot");
        form.setContent("Owner-scoped application");
        form.setCategory(NoteCategory.STUDY);
        return form;
    }
}
