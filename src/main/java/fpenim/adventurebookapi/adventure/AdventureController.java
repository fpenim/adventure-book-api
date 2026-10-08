package fpenim.adventurebookapi.adventure;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import fpenim.adventurebookapi.adventure.dto.AdventureResponse;
import fpenim.adventurebookapi.adventure.dto.AdventureSummaryResponse;
import fpenim.adventurebookapi.adventure.dto.MoveRequest;
import fpenim.adventurebookapi.adventure.dto.StartAdventureRequest;
import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import fpenim.adventurebookapi.book.BookController;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdventureController {

    public static final String USERNAME_HEADER = "X-Username";

    private final AdventureService adventureService;

    public AdventureController(AdventureService adventureService) {
        this.adventureService = adventureService;
    }

    @PostMapping(path = "/adventures/start")
    public ResponseEntity<EntityModel<AdventureResponse>> startAdventure(
            @RequestHeader(USERNAME_HEADER) String username, @Valid @RequestBody StartAdventureRequest request) {
        AdventureResponse adventure = adventureService.startAdventure(username, request.bookId());

        return ResponseEntity.created(linkTo(methodOn(AdventureController.class).getAdventure(adventure.id(), null))
                        .toUri())
                .body(toModel(adventure));
    }

    @GetMapping(path = "/adventures")
    public CollectionModel<EntityModel<AdventureSummaryResponse>> getAdventures(
            @RequestHeader(USERNAME_HEADER) String username) {
        List<EntityModel<AdventureSummaryResponse>> adventureList = adventureService.findAdventures(username).stream()
                .map(adventure -> EntityModel.of(
                        adventure,
                        linkTo(methodOn(AdventureController.class).getAdventure(adventure.id(), null))
                                .withSelfRel()))
                .toList();

        return CollectionModel.of(adventureList);
    }

    @GetMapping(path = "/adventures/{id}")
    public EntityModel<AdventureResponse> getAdventure(
            @PathVariable Long id, @RequestHeader(USERNAME_HEADER) String username) {
        return toModel(adventureService.findAdventure(username, id));
    }

    @PostMapping(path = "/adventures/{id}/move")
    public EntityModel<AdventureResponse> move(
            @PathVariable Long id,
            @RequestHeader(USERNAME_HEADER) String username,
            @Valid @RequestBody MoveRequest request) {
        return toModel(adventureService.move(username, id, request.option()));
    }

    @DeleteMapping(path = "/adventures/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdventure(@PathVariable Long id, @RequestHeader(USERNAME_HEADER) String username) {
        adventureService.deleteAdventure(username, id);
    }

    private static EntityModel<AdventureResponse> toModel(AdventureResponse adventure) {
        EntityModel<AdventureResponse> model = EntityModel.of(
                adventure,
                linkTo(methodOn(AdventureController.class).getAdventure(adventure.id(), null))
                        .withSelfRel(),
                linkTo(methodOn(BookController.class).getBook(adventure.bookId()))
                        .withRel("book"));

        if (adventure.status() == AdventureStatus.IN_PROGRESS) {
            model.add(linkTo(methodOn(AdventureController.class).move(adventure.id(), null, null))
                    .withRel("move"));
        }

        return model;
    }
}
