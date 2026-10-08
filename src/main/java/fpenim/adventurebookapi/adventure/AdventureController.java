package fpenim.adventurebookapi.adventure;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import fpenim.adventurebookapi.adventure.dto.AdventureResponse;
import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import fpenim.adventurebookapi.book.dto.OptionResponse;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/adventures")
public class AdventureController {

    static final String USERNAME_HEADER = "X-Username";

    private final AdventureService adventureService;

    public AdventureController(AdventureService adventureService) {
        this.adventureService = adventureService;
    }

    @PostMapping(path = "/{bookId}")
    public ResponseEntity<EntityModel<AdventureResponse>> start(
            @RequestHeader(USERNAME_HEADER) String username, @PathVariable Long bookId) {
        AdventureService.Started started = adventureService.start(username, bookId);

        return ResponseEntity.status(started.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(toModel(started.adventure()));
    }

    @GetMapping(path = "/{bookId}")
    public EntityModel<AdventureResponse> current(
            @RequestHeader(USERNAME_HEADER) String username, @PathVariable Long bookId) {
        return toModel(adventureService.current(username, bookId));
    }

    @PostMapping(path = "/{bookId}/sections/{sectionId}/options/{position}")
    public EntityModel<AdventureResponse> choose(
            @RequestHeader(USERNAME_HEADER) String username,
            @PathVariable Long bookId,
            @PathVariable Integer sectionId,
            @PathVariable Integer position) {
        return toModel(adventureService.choose(username, bookId, sectionId, position));
    }

    @PostMapping(path = "/{bookId}/restart")
    public EntityModel<AdventureResponse> restart(
            @RequestHeader(USERNAME_HEADER) String username, @PathVariable Long bookId) {
        return toModel(adventureService.restart(username, bookId));
    }

    private static EntityModel<AdventureResponse> toModel(AdventureResponse adventure) {
        Long bookId = adventure.bookId();

        // Options are only actionable while the adventure is in progress.
        if (adventure.status() == AdventureStatus.IN_PROGRESS) {
            List<OptionResponse> options = adventure.section().options();
            for (int position = 0; position < options.size(); position++) {
                options.get(position)
                        .add(linkTo(methodOn(AdventureController.class)
                                        .choose(
                                                null,
                                                bookId,
                                                adventure.section().id(),
                                                position))
                                .withRel("choose"));
            }
        }

        return EntityModel.of(
                adventure,
                linkTo(methodOn(AdventureController.class).current(null, bookId))
                        .withSelfRel(),
                linkTo(methodOn(AdventureController.class).restart(null, bookId))
                        .withRel("restart"));
    }
}
