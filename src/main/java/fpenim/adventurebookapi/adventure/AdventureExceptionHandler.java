package fpenim.adventurebookapi.adventure;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdventureExceptionHandler {

    @ExceptionHandler(AdventureNotFoundException.class)
    public ProblemDetail handleAdventureNotFound(AdventureNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OptionNotFoundException.class)
    public ProblemDetail handleOptionNotFound(OptionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AdventureConflictException.class)
    public ProblemDetail handleAdventureConflict(AdventureConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
