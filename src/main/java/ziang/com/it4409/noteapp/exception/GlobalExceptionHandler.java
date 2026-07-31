package ziang.com.it4409.noteapp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoteNotFoundException.class, NoResourceFoundException.class})
    ModelAndView handleNotFound() {
        ModelAndView view = new ModelAndView("error/404");
        view.setStatus(HttpStatus.NOT_FOUND);
        return view;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class})
    ModelAndView handleBadRequest() {
        ModelAndView view = new ModelAndView("error/400");
        view.setStatus(HttpStatus.BAD_REQUEST);
        return view;
    }

    @ExceptionHandler(Exception.class)
    ModelAndView handleUnexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        ModelAndView view = new ModelAndView("error/500");
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return view;
    }
}
