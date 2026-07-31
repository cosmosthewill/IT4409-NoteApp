package ziang.com.it4409.noteapp.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ziang.com.it4409.noteapp.note.Note;
import ziang.com.it4409.noteapp.note.NoteCategory;
import ziang.com.it4409.noteapp.note.NoteRepository;
import ziang.com.it4409.noteapp.user.User;
import ziang.com.it4409.noteapp.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;
    private User otherUser;
    private Note ownerNote;
    private Note otherNote;

    @BeforeEach
    void setUp() {
        owner = createUser("smoke-owner", "owner@example.com");
        otherUser = createUser("smoke-other", "other@example.com");
        ownerNote = note(owner, "Ownership-safe note");
        otherNote = note(otherUser, "Another user's private note");
    }

    @Test
    void publicLandingAndLoginPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mọi ý tưởng")));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void faviconRoutesWithoutAnApplicationError() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/favicon.svg"));

        mockMvc.perform(get("/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    void unauthenticatedNotesRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/notes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedNotesListAndFragmentRender() throws Exception {
        mockMvc.perform(get("/notes").with(user(owner.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("notes/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ownerNote.getTitle())))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(otherNote.getTitle())
                )));

        mockMvc.perform(get("/notes/fragments").with(user(owner.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("notes/fragments/note-cards :: noteCards"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("notes-fragment")));
    }

    @Test
    void englishLocaleAndDetailPageRender() throws Exception {
        mockMvc.perform(get("/notes").param("lang", "en").with(user(owner.getUsername())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("My notes")));

        mockMvc.perform(get("/notes/{id}", ownerNote.getId()).with(user(owner.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("notes/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ownerNote.getTitle())));
    }

    @Test
    void anotherUsersNoteReturnsSameNotFoundPage() throws Exception {
        mockMvc.perform(get("/notes/{id}", otherNote.getId()).with(user(owner.getUsername())))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void invalidCreateRendersLocalizedFormWithBadRequest() throws Exception {
        mockMvc.perform(post("/notes")
                        .with(user(owner.getUsername()))
                        .with(csrf())
                        .param("title", " ")
                        .param("content", "")
                        .param("category", "STUDY"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("notes/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng nhập tiêu đề")));
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.saveAndFlush(user);
    }

    private Note note(User user, String title) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent("Smoke-test content");
        note.setCategory(NoteCategory.STUDY);
        note.setUser(user);
        return noteRepository.saveAndFlush(note);
    }
}
