package tw.waterballsa.gaas.saboteur.spring.repositories.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.waterballsa.gaas.saboteur.domain.*;
import tw.waterballsa.gaas.saboteur.domain.exceptions.SaboteurGameException;

import static java.util.Objects.requireNonNull;
import static tw.waterballsa.gaas.saboteur.domain.PathCard.*;

/**
 * @author johnny@waterballsa.tw
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardData {
    private Type type;

    // REPAIR
    private ToolName repairToolName;

    // DESTINATION
    private Integer row;
    private Integer col;
    private String name;
    private boolean[] path;

    private Boolean isGold;

    public enum Type {
        REPAIR, MAP, DESTINATION
    }

    public Card toDomain() {
        return switch (type) {
            case REPAIR -> new Repair(requireNonNull(repairToolName));
            case MAP -> new MapCard();
            case DESTINATION -> toPathCard();
        };
    }

    private PathCard toPathCard() {
        return switch (name) {
            case 十字路口 -> 十字路口();
            case T型死路 -> T型死路();
            case 一字型 -> 一字型();
            case 右彎 -> 右彎();
            default -> new PathCard(name, path);
        };
    }

    public static CardData toData(Card card) {
        if (card instanceof Repair r) {
            return toRepairCardData(r);
        } else if (card instanceof PathCard p) {
            return toPathCardData(p);
        } else if (card instanceof MapCard) {
            return toMapCardData();
        }
        throw new SaboteurGameException("unsupported card class " + card.getClass());
    }

    private static CardData toRepairCardData(Repair card) {
        return CardData.builder()
                .type(Type.REPAIR)
                .repairToolName(card.getToolName())
                .build();
    }

    private static CardData toPathCardData(PathCard card) {
        return CardData.builder()
                .type(Type.DESTINATION)
                .name(card.name())
                .path(card.path())
                .build();
    }

    private static CardData toMapCardData() {
        return CardData.builder()
                .type(Type.MAP)
                .build();
    }

}
