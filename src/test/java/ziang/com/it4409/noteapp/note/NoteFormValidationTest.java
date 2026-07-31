package ziang.com.it4409.noteapp.note;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import ziang.com.it4409.noteapp.note.dto.NoteForm;

import static org.assertj.core.api.Assertions.assertThat;

class NoteFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankTitleAndContent() {
        NoteForm form = new NoteForm();
        form.setTitle(" ");
        form.setContent("");
        form.setCategory(NoteCategory.PERSONAL);

        assertThat(validator.validate(form))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("title", "content");
    }

    @Test
    void rejectsMissingCategory() {
        NoteForm form = new NoteForm();
        form.setTitle("Valid title");
        form.setContent("Valid content");

        assertThat(validator.validate(form))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("category");
    }
}
