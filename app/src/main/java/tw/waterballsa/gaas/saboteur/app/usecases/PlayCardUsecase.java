package tw.waterballsa.gaas.saboteur.app.usecases;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import tw.waterballsa.gaas.saboteur.app.outport.SaboteurGameRepository;
import tw.waterballsa.gaas.saboteur.domain.MapCard;
import tw.waterballsa.gaas.saboteur.domain.PathCard;
import tw.waterballsa.gaas.saboteur.domain.Repair;
import tw.waterballsa.gaas.saboteur.domain.SaboteurGame;
import tw.waterballsa.gaas.saboteur.domain.events.DomainEvent;
import tw.waterballsa.gaas.saboteur.domain.exceptions.SaboteurGameException;

import javax.inject.Named;
import java.util.List;

import static java.lang.String.format;

/**
 * @author johnny@waterballsa.tw
 */
@Named
@RequiredArgsConstructor
public class PlayCardUsecase { /*領域模型的使用案例*/
    private final SaboteurGameRepository saboteurGameRepository;

    public void execute(PlayCardRequest request, Presenter presenter) {
        // 查
        SaboteurGame game = findGame(request);

        List<DomainEvent> events;

        // TDD: write test  -> write just enough code to pass the test -> refactor

        if (request.destinationCardIndex != null) {
            events = game.playCard(new MapCard.Parameters(request.playerId, request.handIndex, request.destinationCardIndex));
        } else if (request.row != null && request.col != null) {
            events = game.playCard(new PathCard.Parameters(request.playerId, request.handIndex, request.row, request.col, request.flipped));
        } else if (request.targetPlayerId != null) {
            events = game.playCard(new Repair.Parameters(request.playerId, request.handIndex, request.targetPlayerId));
        } else {
            throw new IllegalArgumentException("Cannot recognize the type of played card.");
        }

        // 存
        saboteurGameRepository.save(game);

        // 推
        presenter.present(events);
    }

    private SaboteurGame findGame(PlayCardRequest request) {
        String gameId = request.getGameId();
        return saboteurGameRepository.findById(gameId)
                .orElseThrow(() -> new SaboteurGameException(format("Game {%s} not found.", gameId)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayCardRequest {
        private String gameId;
        private String playerId;
        private int handIndex;
        private String targetPlayerId;

        private Integer destinationCardIndex;

        private Integer row, col;

        private Boolean flipped;
    }

    public interface Presenter {
        void present(List<DomainEvent> events);
    }

}
