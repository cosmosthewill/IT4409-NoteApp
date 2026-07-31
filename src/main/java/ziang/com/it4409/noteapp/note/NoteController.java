package ziang.com.it4409.noteapp.note;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ziang.com.it4409.noteapp.note.dto.NoteForm;

@Controller
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @ModelAttribute
    void commonModel(Model model) {
        model.addAttribute("categories", NoteCategory.values());
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) NoteCategory category,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails principal,
            Model model
    ) {
        Page<Note> notesPage = noteService.search(principal.getUsername(), q, category, page);
        model.addAttribute("notesPage", notesPage);
        model.addAttribute("query", q.trim());
        model.addAttribute("selectedCategory", category);
        return "notes/list";
    }

    @GetMapping("/fragments")
    public String fragments(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) NoteCategory category,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails principal,
            Model model
    ) {
        model.addAttribute("notesPage", noteService.search(principal.getUsername(), q, category, page));
        return "notes/fragments/note-cards :: noteCards";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("noteForm", new NoteForm());
        model.addAttribute("formMode", "create");
        return "notes/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("noteForm") NoteForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes,
            HttpServletResponse response,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            model.addAttribute("formMode", "create");
            return "notes/form";
        }

        Note note = noteService.create(principal.getUsername(), form);
        redirectAttributes.addFlashAttribute("successMessage", "note.created");
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model
    ) {
        model.addAttribute("note", noteService.getOwnedNote(principal.getUsername(), id));
        return "notes/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model
    ) {
        Note note = noteService.getOwnedNote(principal.getUsername(), id);
        model.addAttribute("note", note);
        model.addAttribute("noteForm", NoteForm.from(note));
        model.addAttribute("formMode", "edit");
        return "notes/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("noteForm") NoteForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes,
            HttpServletResponse response,
            Model model
    ) {
        Note note = noteService.getOwnedNote(principal.getUsername(), id);
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            model.addAttribute("note", note);
            model.addAttribute("formMode", "edit");
            return "notes/form";
        }

        noteService.update(principal.getUsername(), id, form);
        redirectAttributes.addFlashAttribute("successMessage", "note.updated");
        return "redirect:/notes/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        noteService.delete(principal.getUsername(), id);
        redirectAttributes.addFlashAttribute("successMessage", "note.deleted");
        return "redirect:/notes";
    }

    @PostMapping("/{id}/pin")
    public String togglePin(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        noteService.togglePinned(principal.getUsername(), id);
        redirectAttributes.addFlashAttribute("successMessage", "note.pinChanged");
        return "redirect:/notes";
    }
}
