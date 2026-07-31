package ziang.com.it4409.noteapp.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String landing(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/notes";
        }
        return "landing";
    }

    @GetMapping("/favicon.ico")
    public String legacyFavicon() {
        return "redirect:/favicon.svg";
    }
}
