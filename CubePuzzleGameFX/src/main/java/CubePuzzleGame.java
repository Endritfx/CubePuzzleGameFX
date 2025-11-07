import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.Random;

public class CubePuzzleGame extends Application {

    private static final int TILE_SIZE = 60;
    private static final int GRID_WIDTH = 8;
    private static final int GRID_HEIGHT = 8;
    private static final int MAX_LEVEL = 10;

    private int cubeX, cubeY;
    private int goalX, goalY;
    private boolean[][] platform;

    private int level = 0;
    private boolean gameOver = false;
    private boolean gameWin = false;
    private boolean moving = false;
    private boolean inMenu = true;
    private boolean inSkinsPage = false; // për skins page

    private int movesThisLevel = 0;
    private int score = 0;

    private double animX, animY;
    private double targetX, targetY;
    private double animSpeed = 6;

    private Random random = new Random();
    private Canvas canvas;
    private GraphicsContext gc;

    // Array që ruan lëvizjet më të mira për çdo level
    private int[] bestMoves = new int[MAX_LEVEL];
    private boolean[] levelPlayed = new boolean[MAX_LEVEL];

    // Ngjyra e kubit
    private Color cubeColor = Color.CORNFLOWERBLUE;

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(GRID_WIDTH * TILE_SIZE, GRID_HEIGHT * TILE_SIZE);
        gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Cube Puzzle Game (10 Levels)");
        stage.setResizable(false);
        stage.show();

        showMenu();

        // Klikimi me mouse
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (inMenu) {
                // Butoni Skins
                int skinWidth = 80;
                int skinHeight = 30;
                int skinX = (int)canvas.getWidth() - skinWidth - 20;
                int skinY = 20;
                if (e.getX() >= skinX && e.getX() <= skinX + skinWidth &&
                        e.getY() >= skinY && e.getY() <= skinY + skinHeight) {
                    openSkinsPage();
                    return;
                }

                // Zgjedhja e niveleve
                int size = 50;
                int gapX = 30;
                int gapY = 30;
                int startX = 50;
                int startY = 250;

                for (int i = 0; i < MAX_LEVEL; i++) {
                    int row = i / 5;
                    int col = i % 5;
                    int x = startX + col * (size + gapX);
                    int y = startY + row * (size + gapY);

                    if (e.getX() >= x && e.getX() <= x + size &&
                            e.getY() >= y && e.getY() <= y + size) {
                        inMenu = false;
                        loadLevel(i);
                        break;
                    }
                }
            }

            if (inSkinsPage) {
                drawSkinsPage(e.getX(), e.getY());
            }
        });

        // Tastiera
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.M) {
                inMenu = true;
                inSkinsPage = false;
                showMenu();
                return;
            }

            if (e.getCode() == KeyCode.R) {
                if (gameOver) loadLevel(level);
                else if (gameWin && level == MAX_LEVEL - 1) { inMenu = true; showMenu(); }
                else if (gameWin) nextLevel();
            }

            if (gameOver || gameWin || moving || inMenu || inSkinsPage) return;

            int newX = cubeX, newY = cubeY;
            if (e.getCode() == KeyCode.LEFT) newX--;
            if (e.getCode() == KeyCode.RIGHT) newX++;
            if (e.getCode() == KeyCode.UP) newY--;
            if (e.getCode() == KeyCode.DOWN) newY++;

            tryMove(newX, newY);
        });

        new AnimationTimer() {
            public void handle(long now) {
                if (!inMenu && !inSkinsPage) { update(); draw(); }
                if (inSkinsPage) drawSkinsPage(-1, -1); // rifreskon skins page vazhdimisht
            }
        }.start();
    }

    private void tryMove(int newX, int newY) {
        // Kontrollo nëse kubi do të dalë jashtë kufijve
        if (newX < 0 || newY < 0 || newX >= GRID_WIDTH || newY >= GRID_HEIGHT) {
            // Nuk rritet movesThisLevel, nuk ndryshon pozicioni
            return;
        }

        // Kontrollo nëse ka platformë
        if (!platform[newX][newY]) {
            gameOver = true; // vetëm kur tenton të shkel tile të zbrazët
            return;
        }

        // Në këtë pikë, lëvizja është e vlefshme
        moving = true;
        targetX = newX * TILE_SIZE;
        targetY = newY * TILE_SIZE;
        cubeX = newX;
        cubeY = newY;

        // Vetëm kur lëviz kubi rritet counter-i
        movesThisLevel++;

        // Kontrollo goal
        if (cubeX == goalX && cubeY == goalY) {
            gameWin = true;
            if (!levelPlayed[level] || movesThisLevel < bestMoves[level]) {
                bestMoves[level] = movesThisLevel;
            }
            levelPlayed[level] = true;
        }
    }


    private void update() {
        if (moving) {
            double dx = targetX - animX;
            double dy = targetY - animY;
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist < animSpeed) {
                animX = targetX;
                animY = targetY;
                moving = false;
                if (gameWin) {
                    int minMoves = 14;
                    if (movesThisLevel <= minMoves) score += 100;
                    else if (movesThisLevel <= minMoves + 2) score += 50;
                    else score += 10;
                    nextLevel();
                }
            } else {
                animX += animSpeed * Math.signum(dx);
                animY += animSpeed * Math.signum(dy);
            }
        }
    }

    private boolean pathExists(int startX, int startY, int goalX, int goalY) {
        boolean[][] visited = new boolean[GRID_WIDTH][GRID_HEIGHT];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];
            if (x == goalX && y == goalY) return true;
            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (nx>=0 && ny>=0 && nx<GRID_WIDTH && ny<GRID_HEIGHT && platform[nx][ny] && !visited[nx][ny]) {
                    visited[nx][ny]=true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }

    private void loadLevel(int levelNum) {
        platform = new boolean[GRID_WIDTH][GRID_HEIGHT];
        while (true) {
            for (int x=0;x<GRID_WIDTH;x++) for(int y=0;y<GRID_HEIGHT;y++) platform[x][y]=true;
            int holes = Math.min((levelNum+1)*4, GRID_WIDTH*GRID_HEIGHT/2);
            for (int i=0;i<holes;i++) {
                int hx, hy;
                do { hx=random.nextInt(GRID_WIDTH); hy=random.nextInt(GRID_HEIGHT); }
                while ((hx==0 && hy==0)||(hx==GRID_WIDTH-1 && hy==GRID_HEIGHT-1)||
                        (Math.abs(hx-0)<=1 && Math.abs(hy-0)<=1)||(Math.abs(hx-(GRID_WIDTH-1))<=1 && Math.abs(hy-(GRID_HEIGHT-1))<=1));
                platform[hx][hy]=false;
            }
            if (pathExists(0,0,GRID_WIDTH-1,GRID_HEIGHT-1)) break;
        }

        cubeX=0; cubeY=0;
        goalX=GRID_WIDTH-1; goalY=GRID_HEIGHT-1;
        animX=cubeX*TILE_SIZE; animY=cubeY*TILE_SIZE;
        level=levelNum; gameOver=false; gameWin=false; moving=false; movesThisLevel=0;
    }

    private void nextLevel() { if (level<MAX_LEVEL-1) loadLevel(level+1); else gameWin=true; }

    private void draw() {
        gc.setFill(Color.rgb(30,30,30));
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for (int x=0;x<GRID_WIDTH;x++)
            for (int y=0;y<GRID_HEIGHT;y++)
                if (platform[x][y]) { gc.setFill(Color.DARKGRAY); gc.fillRect(x*TILE_SIZE,y*TILE_SIZE,TILE_SIZE-2,TILE_SIZE-2); }

        gc.setFill(Color.LIGHTGREEN);
        gc.fillRect(goalX*TILE_SIZE+10, goalY*TILE_SIZE+10, TILE_SIZE-20, TILE_SIZE-20);

        gc.setFill(cubeColor);
        gc.fillRoundRect(animX+8, animY+8, TILE_SIZE-16, TILE_SIZE-16,10,10);

        gc.setFill(Color.WHITE);
        gc.fillText("Level: "+(level+1),10,20);
        gc.fillText("Score: "+score,10,40);
        gc.fillText("Moves this level: "+movesThisLevel,10,60);

        if (gameOver) { gc.setFill(Color.RED); gc.fillText("Game Over! Press R to restart",150,240); }
        if (gameWin && level==MAX_LEVEL-1) { gc.setFill(Color.LIME); gc.fillText("You Win All Levels! Final Score: "+score+" Press M for menu.",50,280); }
    }

    private void showMenu() {
        inMenu = true;
        inSkinsPage = false;
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.LIGHTBLUE);
        gc.fillText("Cube Puzzle Game - 10 Levels", 20, 50);

        gc.setFill(Color.WHITE);
        gc.fillText("Use Arrow Keys to move the cube", 20, 80);
        gc.fillText("Press R to Restart Level", 20, 110);
        gc.fillText("Press M to return to Menu", 20, 140);

        // Butoni Skins
        int skinWidth = 80;
        int skinHeight = 30;
        int skinX = (int)canvas.getWidth() - skinWidth - 20;
        int skinY = 20;

        gc.setFill(Color.DARKBLUE);
        gc.fillRect(skinX, skinY, skinWidth, skinHeight);
        gc.setFill(Color.WHITE);
        gc.fillText("Skins", skinX + 20, skinY + 20);

        int size = 60;
        int gapX = 20;
        int gapY = 30;
        int startX = 50;
        int startY = 250;
        int[] levelMinMoves = {14,14,14,14,14,14,14,14,14,14};

        for (int i = 0; i < MAX_LEVEL; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = startX + col * (size + gapX);
            int y = startY + row * (size + gapY);

            gc.setFill(Color.DARKGRAY);
            gc.fillRect(x, y, size, size);
            gc.setFill(Color.WHITE);
            gc.fillText("" + (i + 1), x + 5, y + 15);

            int stars = 3;
            if (levelPlayed[i]) {
                int diff = bestMoves[i] - levelMinMoves[i];
                if (diff <= 0) stars = 3;
                else if (diff <= 2) stars = 2;
                else stars = 1;
            }

            int starSize = 8;
            int starSpacing = 12;
            for (int s = 0; s < 3; s++) {
                int starX = x + 10 + s*starSpacing;
                int starY = y + size - 20;
                gc.setFill(levelPlayed[i] ? (s < stars ? Color.GOLD : Color.LIGHTGRAY) : Color.WHITE);
                gc.fillOval(starX, starY, starSize, starSize);
            }
        }
    }

    private void openSkinsPage() {
        inSkinsPage = true;
        inMenu = false;
        drawSkinsPage(-1, -1);
    }

    private boolean greenUnlocked = false;
    private boolean orangeUnlocked = false;
    private boolean purpleUnlocked = false;
    private boolean goldUnlocked = false;
    private String message = "";
    private long messageTime = 0;

    private void drawSkinsPage(double clickX, double clickY) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Titulli
        gc.setFill(Color.LIGHTBLUE);
        gc.fillText("Skins Page - Choose your cube color", 50, 100);

        // Butoni Back
        int backWidth = 80;
        int backHeight = 30;
        int backX = 20;
        int backY = 20;

        gc.setFill(Color.DARKBLUE);
        gc.fillRect(backX, backY, backWidth, backHeight);
        gc.setFill(Color.WHITE);
        gc.fillText("Back", backX + 20, backY + 20);

        // ------------------- Trego Score/Pikët e Lojtarit -------------------
        gc.setFill(Color.YELLOW);
        gc.fillText("Points: " + score, canvas.getWidth() - 120, 50);

        // Ngjyrat bazë të lira
        Color[] freeColors = {Color.CORNFLOWERBLUE, Color.RED};
        int baseSize = 50;
        int baseGap = 20;
        int baseStartX = 50;
        int baseStartY = 200;

        gc.setFill(Color.WHITE);
        gc.fillText("Free Colors:", baseStartX, baseStartY - 10);

        for (int i = 0; i < freeColors.length; i++) {
            int x = baseStartX + i * (baseSize + baseGap);
            int y = baseStartY;
            gc.setFill(freeColors[i]);
            gc.fillRect(x, y, baseSize, baseSize);

            if (cubeColor.equals(freeColors[i])) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(3);
                gc.strokeRect(x - 3, y - 3, baseSize + 6, baseSize + 6);
            }

            if (clickX >= x && clickX <= x + baseSize && clickY >= y && clickY <= y + baseSize) {
                cubeColor = freeColors[i];
                drawSkinsPage(-1, -1);
                return;
            }
        }

        // Skina që kërkojnë points
        String[] skinNames = {"Green Cube", "Orange Cube", "Purple Cube", "Gold Cube"};
        Color[] skinColors = {Color.GREEN, Color.ORANGE, Color.PURPLE, Color.GOLD};
        int[] skinPrices = {100, 200, 300, 400};
        boolean[] unlocked = {greenUnlocked, orangeUnlocked, purpleUnlocked, goldUnlocked};

        int skinSize = 60;
        int skinGap = 50;
        int skinStartY = baseStartY + baseSize + 80;

        gc.setFill(Color.WHITE);
        gc.fillText("Unlockable Skins (buy with points):", baseStartX, skinStartY - 10);

        for (int i = 0; i < skinColors.length; i++) {
            int x = baseStartX + i * (skinSize + skinGap);
            int y = skinStartY;

            gc.setFill(skinColors[i]);
            gc.fillRect(x, y, skinSize, skinSize);

            gc.setFill(Color.WHITE);
            gc.fillText(skinNames[i], x, y + skinSize + 15);

            if (unlocked[i]) gc.fillText("Unlocked", x, y + skinSize + 30);
            else gc.fillText(skinPrices[i] + " pts", x, y + skinSize + 30);

            if (cubeColor.equals(skinColors[i])) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(3);
                gc.strokeRect(x - 3, y - 3, skinSize + 6, skinSize + 6);
            }

            // Klikimi për skin-et me points
            if (clickX >= x && clickX <= x + skinSize && clickY >= y && clickY <= y + skinSize) {
                if (unlocked[i]) {
                    cubeColor = skinColors[i];
                } else {
                    if (score >= skinPrices[i]) {
                        score -= skinPrices[i];
                        if (i == 0) greenUnlocked = true;
                        if (i == 1) orangeUnlocked = true;
                        if (i == 2) purpleUnlocked = true;
                        if (i == 3) goldUnlocked = true;
                        message = "Unlocked " + skinNames[i] + "!";
                        messageTime = System.currentTimeMillis();
                    } else {
                        message = "Not enough points!";
                        messageTime = System.currentTimeMillis();
                    }
                }
                drawSkinsPage(-1, -1);
                return;
            }
        }

        // Klikimi në "Back"
        if (clickX >= backX && clickX <= backX + backWidth &&
                clickY >= backY && clickY <= backY + backHeight) {
            inSkinsPage = false;
            showMenu();
            return;
        }

        // Mesazh për pak sekonda
        if (!message.isEmpty() && System.currentTimeMillis() - messageTime < 2000) {
            gc.setFill(Color.YELLOW);
            gc.fillText(message, 50, canvas.getHeight() - 40);
        }
    }

    public static void main(String[] args) { launch(); }
}
