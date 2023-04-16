package tw.waterballsa.gaas.saboteur.spring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tw.waterballsa.gaas.saboteur.app.outport.SaboteurGameRepository;
import tw.waterballsa.gaas.saboteur.domain.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tw.waterballsa.gaas.saboteur.domain.PathCard.T型死路;
import static tw.waterballsa.gaas.saboteur.domain.PathCard.十字路口;
import static tw.waterballsa.gaas.saboteur.domain.builders.Players.defaultPlayer;
import static tw.waterballsa.gaas.saboteur.domain.builders.Players.defaultPlayerBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class SaboteurGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaboteurGameRepository gameRepository;

    // ATDD (1) 先寫驗收測試程式 （2) ------------
    @Test
    public void 修好其中一個工具耶() throws Exception {
        Player A = new Player(
                "A",
                emptyList(),
                new Tool(ToolName.MINE_CART, true),
                new Tool(ToolName.LANTERN, true),
                new Tool(ToolName.PICK, false)
        );
        Player B = new Player(
                "B",
                emptyList(),
                new Tool(ToolName.MINE_CART, true),
                new Tool(ToolName.LANTERN, true),
                new Tool(ToolName.PICK, true)
        );
        B.addHandCard(new Repair(ToolName.PICK));

        Player C = new Player("C",
                emptyList(),
                new Tool(ToolName.MINE_CART, true),
                new Tool(ToolName.LANTERN, true),
                new Tool(ToolName.PICK, true));

        SaboteurGame game = givenGameStarted(A, B, C);

        mockMvc.perform(post("/api/games/{gameId}:playCard", game.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{" +
                                "  \"playerId\": \"B\",\n" +
                                "  \"handIndex\": 0,\n" +
                                "  \"targetPlayerId\": \"A\"\n" +
                                "}"))
                .andExpect(status().isNoContent());

        var actualGame = findGameById(game.getId());
        Player actualA = actualGame.getPlayer("A");

        assertTrue(actualA.getTool(ToolName.MINE_CART).isAvailable());
        assertTrue(actualA.getTool(ToolName.LANTERN).isAvailable());
        assertTrue(actualA.getTool(ToolName.PICK).isAvailable());
    }

    private SaboteurGame givenGameStarted(SaboteurGame game) {
        return gameRepository.save(game);
    }

    private SaboteurGame givenGameStarted(Player... players) {
        return gameRepository.save(new SaboteurGame(asList(players)));
    }

    private SaboteurGame findGameById(String gameId) {
        // 從 repo 查出 game
        return gameRepository.findById(gameId).orElseThrow();
    }

    @Test
    public void 都好的硬要修() throws Exception {
        Player A = defaultPlayer("A");
        Player B = defaultPlayer("B");
        B.addHandCard(new Repair(ToolName.PICK));

        Player C = defaultPlayer("C");

        SaboteurGame game = givenGameStarted(A, B, C);

        mockMvc.perform(post("/api/games/{gameId}:playCard", game.getId())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {  "playerId": "B",
                                  "handIndex": 0,
                                  "targetPlayerId": "A"
                                }"""))
                .andExpect(status().isBadRequest());

        var actualGame = findGameById(game.getId());
        Player actualA = actualGame.getPlayer("A");

        assertTrue(actualA.getTool(ToolName.MINE_CART).isAvailable());
        assertTrue(actualA.getTool(ToolName.LANTERN).isAvailable());
        assertTrue(actualA.getTool(ToolName.PICK).isAvailable());
    }

    /**
     * Given
     * 一玩家 A，終點 2 中有金礦
     * A 持有地圖卡
     * 目前輪到 A
     * When
     * A玩家對 終點2 使用地圖卡
     * Then
     * A玩家看到終點2 上有金礦
     */
    @Test
    public void 看終點底下有無金礦喔() throws Exception {
        Player A = defaultPlayerBuilder("A")
                .hand(new MapCard()).build();
        Player B = defaultPlayer("B");
        Player C = defaultPlayer("C");

        SaboteurGame game = new SaboteurGame(asList(A, B, C)); //givenGameStarted?
        game.setGoldInDestinationCard(2);
        game = givenGameStarted(game);

        //A玩家對 終點2 使用地圖卡
        mockMvc.perform(post("/api/games/{gameId}:playCard", game.getId())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {  "playerId": "A",
                                  "handIndex": 0,
                                  "destinationCardIndex": 2
                                }"""))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.isGold").value(true));

    }

    /*

        驗收測試的職責：定義開發進度
        遊戲的一連串的遊玩過程

        Given 三位玩家 A, B, C


        When-Then:
        輪到 A 出一張道路卡，可以成功連接回起點，放置成功
        輪到 B 出一張道路卡，可以成功連接回起點，放置成功
        輪到 C 出一張道路卡，無法成功連接回起點，放置失敗
        輪到 C 出另一張道路卡，可以成功連接回起點，放置成功

        Given / When 測項 / Then 驗證測項
     */
    @Test
    @SuppressWarnings("NonAsciiCharacters")
    void test迷宮案例1() throws Exception {
        // Given
        Player A = defaultPlayerBuilder("A")
                .hand(十字路口())
                .build();
        Player B = defaultPlayerBuilder("B")
                .hand(T型死路())
                .build();
        Player C = defaultPlayerBuilder("C")
                .hand(PathCard.一字型())
                .hand(PathCard.右彎())
                .build();

        SaboteurGame game = givenGameStarted(A, B, C);

        // When -- Then
        //  輪到 A 出一張十字路口，可以成功連接回起點，放置成功
        playPathCard(game, "A", 0, 0, 1, false)
                .andExpect(status().is2xxSuccessful());

        // 輪到 B 出一張T型死路，可以成功連接回起點，放置成功
        playPathCard(game, "B", 0, 0, 2, false)
                .andExpect(status().is2xxSuccessful());

        // 輪到 C 出一張一字型卡，無法成功連接回起點，放置失敗
        playPathCard(game, "C", 0, 0, 3, false)
                .andExpect(status().isBadRequest());

        //輪到 C 出另一張L型（翻轉右彎）卡，可以成功連接回起點，放置成功
        playPathCard(game, "C", 1, -1, 1, true)
                .andExpect(status().is2xxSuccessful());

        var actualGame = gameRepository.findById(game.getId()).orElseThrow();
        Maze maze = actualGame.getMaze();

        assertEquals(十字路口(), maze.getPath(0, 1)
                .map(Path::getPathCard).orElseThrow());

        assertEquals(T型死路(), maze.getPath(0, 2)
                .map(Path::getPathCard).orElseThrow());

        Path actual右彎 = maze.getPath(-1, 1).orElseThrow();
        assertTrue(actual右彎.isFlipped());
    }

    private ResultActions playPathCard(SaboteurGame game, String playerId, int handIndex, int row, int col, boolean flipped) throws Exception {
        return mockMvc.perform(post("/api/games/{gameId}:playCard", game.getId())
                .contentType(APPLICATION_JSON)
                .content(format("""
                            {"playerId": "%s",
                            "handIndex": %d,
                            "row": %d,
                            "col": %d,
                            "flipped": %b}
                        """, playerId, handIndex, row, col, flipped)));
    }

}