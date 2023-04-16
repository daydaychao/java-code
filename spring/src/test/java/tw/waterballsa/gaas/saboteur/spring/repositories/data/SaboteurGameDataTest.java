package tw.waterballsa.gaas.saboteur.spring.repositories.data;

import org.junit.jupiter.api.Test;
import tw.waterballsa.gaas.saboteur.domain.*;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static tw.waterballsa.gaas.saboteur.domain.builders.Players.defaultPlayer;
import static tw.waterballsa.gaas.saboteur.domain.builders.Players.defaultPlayerBuilder;
import static tw.waterballsa.gaas.saboteur.spring.repositories.data.SaboteurGameData.toData;

class SaboteurGameDataTest {

    // 可選建構子
    @Test
    void testToData() {
        // given
        final String ID = "GameId";
        var game = new SaboteurGame(
                ID, asList(defaultPlayerBuilder("A")
                        .hands(asList(
                                new Repair(ToolName.PICK),
                                new Repair(ToolName.LANTERN),
                                new MapCard()
                        )).build(),
                defaultPlayer("B"),
                defaultPlayer("C")
        ),
                new Maze(List.of(new Path(0, 0, PathCard.十字路口()),
                        new Path(0, 1, PathCard.十字路口(), true),
                        new Path(0, 2, PathCard.T型死路()),
                        new Path(1, 1, PathCard.右彎()),
                        new Path(-1, 1, PathCard.右彎(), true))));
        game.setGoldInDestinationCard(1);

        SaboteurGameData data = toData(game);

        // assert
        assertEquals(game.getId(), data.getId());
        List<PlayerData> players = data.getPlayers();
        assertEquals(3, players.size());


        assertAll("players hand cards assertion", () -> {
            PlayerData A = players.get(0);
            assertEquals("A", A.getId());
            CardData ACard0 = A.getHands().get(0);
            CardData ACard1 = A.getHands().get(1);
            CardData ACard2 = A.getHands().get(2);
            assertEquals(3, A.getHands().size());
            assertEquals(CardData.Type.REPAIR, ACard0.getType());
            assertEquals(CardData.Type.REPAIR, ACard1.getType());
            assertEquals(CardData.Type.MAP, ACard2.getType());
            assertEquals(ToolName.PICK, ACard0.getRepairToolName());
            assertEquals(ToolName.LANTERN, ACard1.getRepairToolName());
        });

        players.forEach(this::assertThreeAvailableTools);

        assertAll("one destination card must be golden.", () -> {
            List<PathData> destinationCards = data.getDestinations();
            assertFalse(destinationCards.get(0).isGold());
            assertTrue(destinationCards.get(1).isGold());
            assertFalse(destinationCards.get(2).isGold());
        });

        MazeData maze = data.getMaze();
        Set<PathData> actualPaths = new HashSet<>(maze.getPaths());
        var expectedPaths = Set.of(
                new PathData(0, 0, PathCard.十字路口, false, null),
                new PathData(0, 1, PathCard.十字路口, true, null),
                new PathData(0, 2, PathCard.T型死路, false, null),
                new PathData(1, 1, PathCard.右彎, false, null),
                new PathData(-1, 1, PathCard.右彎, true, null));
        assertEquals(expectedPaths, actualPaths);
    }

    private void assertThreeAvailableTools(PlayerData player) {
        assertAll("player should have three available tools", () -> {
            var distinctTools = player.getTools().stream().map(ToolData::getToolName).collect(toSet());
            assertEquals(3, distinctTools.size());
            var toolSet = Set.of(ToolName.MINE_CART, ToolName.LANTERN, ToolName.PICK);
            assertEquals(toolSet, distinctTools);
        });
    }

    @Test
    void testToDomain() {
        List<ToolData> tools = asList(new ToolData(ToolName.LANTERN, true),
                new ToolData(ToolName.PICK, true),
                new ToolData(ToolName.MINE_CART, true));
        SaboteurGameData data = new SaboteurGameData("G",
                asList(new PlayerData("A", tools,
                                asList(CardData.toData(new Repair(ToolName.LANTERN)),
                                        CardData.toData(new MapCard()))),
                        new PlayerData("B", tools, singletonList(CardData.toData(new Repair(ToolName.MINE_CART)))),
                        new PlayerData("C", tools, singletonList(CardData.toData(new Repair(ToolName.PICK))))),
                new MazeData(List.of(
                        new PathData(0, 0, PathCard.十字路口, false, null),
                        new PathData(0, 1, PathCard.十字路口, true, null),
                        new PathData(0, 2, PathCard.T型死路, false, null),
                        new PathData(1, 1, PathCard.右彎, false, null),
                        new PathData(-1, 1, PathCard.右彎, true, null))),
                asList(PathData.toData(new Destination(8, 0, false)),
                        PathData.toData(new Destination(8, 2, true)),
                        PathData.toData(new Destination(8, 4, false))));

        SaboteurGame game = data.toDomain();
        List<Player> players = game.getPlayers();
        Player A = players.get(0);
        Player B = players.get(1);
        Player C = players.get(2);

        var destinationCards = game.getDestinations();
        assertEquals(3, players.size());
        assertFalse(destinationCards.get(0).isGold());
        assertTrue(destinationCards.get(1).isGold());
        assertFalse(destinationCards.get(2).isGold());

        players.forEach(this::assertThreeAvailableTools);
        assertHasRepairCard(A, 0, ToolName.LANTERN);
        assertHasRepairCard(B, 0, ToolName.MINE_CART);
        assertHasRepairCard(C, 0, ToolName.PICK);
        assertHasMapCard(A, 1);

        Maze maze = game.getMaze();
        Set<Path> actualPaths = new HashSet<>(maze.getPaths());
        var expectedPaths = Set.of(new Path(0, 0, PathCard.十字路口()),
                new Path(0, 1, PathCard.十字路口(), true),
                new Path(0, 2, PathCard.T型死路()),
                new Path(1, 1, PathCard.右彎()),
                new Path(-1, 1, PathCard.右彎(), true));
        assertEquals(expectedPaths, actualPaths);
    }

    private void assertHasRepairCard(Player player, int handIndex, ToolName repairToolName) {
        Card card = player.getHands().get(handIndex);
        assertEquals(Repair.class, card.getClass());
        assertEquals(repairToolName, ((Repair) card).getToolName());
    }

    private void assertHasMapCard(Player player, int handIndex) {
        Card card = player.getHands().get(handIndex);
        assertEquals(MapCard.class, card.getClass());
    }

    private void assertThreeAvailableTools(Player player) {
        Tool[] tools = player.getTools();
        assertEquals(3, tools.length);
        assertTrue(stream(tools).allMatch(Tool::isAvailable));
    }
}