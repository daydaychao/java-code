package tw.waterballsa.gaas.saboteur.spring.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.waterballsa.gaas.saboteur.app.usecases.PlayCardUsecase;
import tw.waterballsa.gaas.saboteur.domain.events.DestinationCardRevealedEvent;
import tw.waterballsa.gaas.saboteur.domain.events.DomainEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import static org.springframework.http.ResponseEntity.noContent;
import static tw.waterballsa.gaas.saboteur.app.usecases.PlayCardUsecase.PlayCardRequest;

/**
 * @author johnny@waterballsa.tw
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class SaboteurController {
    private final PlayCardUsecase playCardUsecase;

    @PostMapping("/{gameId}:playCard")
    public ResponseEntity<?> playCard(@PathVariable String gameId,
                                      @RequestBody Request request) {
        var presenter = new PlayCardPresenter();
        playCardUsecase.execute(request.toRequest(gameId), presenter);

        return presenter.getViewModel()
                .map(ResponseEntity::ok)
                .orElseGet(noContent()::build);
    }

    @Value
    public static class Request {
        String playerId;
        int handIndex;

        // play repair card
        String targetPlayerId;

        // play map card
        Integer destinationCardIndex;

        // play path card
        Integer row, col;

        Boolean flipped;

        public PlayCardRequest toRequest(String gameId) {
            return new PlayCardRequest(gameId, playerId, handIndex, targetPlayerId, destinationCardIndex,
                    row, col, requireNonNullElse(flipped, false));
        }
    }
}

class PlayCardPresenter implements PlayCardUsecase.Presenter {
    private DestinationCardRevealedViewModel viewModel;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationCardRevealedViewModel {
        @JsonProperty("isGold")
        private boolean isGold;
    }

    @Override
    public void present(List<DomainEvent> events) {
        viewModel = getEvent(events, DestinationCardRevealedEvent.class)
                .map(e -> new DestinationCardRevealedViewModel(e.isGold))
                .orElse(null);
    }

    public Optional<DestinationCardRevealedViewModel> getViewModel() {
        return ofNullable(viewModel);
    }

    @SuppressWarnings("unchecked")
    private static <T extends DomainEvent> Optional<T> getEvent(List<DomainEvent> events,
                                                                Class<T> type) {
        return events.stream()
                .filter(e -> type.isAssignableFrom(e.getClass()))
                .map(e -> (T) e)
                .findFirst();
    }
}
