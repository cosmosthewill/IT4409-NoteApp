package ziang.com.it4409.noteapp.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ziang.com.it4409.noteapp.note.Note;
import ziang.com.it4409.noteapp.note.NoteCategory;

@Getter
@Setter
public class NoteForm {

    @NotBlank(message = "{validation.note.title.required}")
    @Size(max = 150, message = "{validation.note.title.size}")
    private String title;

    @NotBlank(message = "{validation.note.content.required}")
    @Size(max = 10_000, message = "{validation.note.content.size}")
    private String content;

    @NotNull(message = "{validation.note.category.required}")
    private NoteCategory category;

    public static NoteForm from(Note note) {
        NoteForm form = new NoteForm();
        form.setTitle(note.getTitle());
        form.setContent(note.getContent());
        form.setCategory(note.getCategory());
        return form;
    }
}
