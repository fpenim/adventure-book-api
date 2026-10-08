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

    @ExceptionHandler(InvalidOptionException.class)
    public ProblemDetail handleInvalidOption(InvalidOptionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AdventureOverException.class)
    public ProblemDetail handleAdventureOver(AdventureOverException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
