package tetris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class TetrisPanel extends JPanel implements ActionListener, KeyListener {

    static final int COLS = GameBoard.COLS;
    static final int ROWS = GameBoard.ROWS;
    static final int BLOCK = 30;
    static final int OFFSET_Y = 40;

    GameBoard gameBoard = new GameBoard();
    Timer timer;

    Piece currentPiece;
    int[][] nextBlock;
    Color nextColor;

    boolean running = false;
    boolean gameOver = false;
    boolean landingAnim = false;

    boolean spinActive = false;
    long spinStartTime, lastSpinTime;

    boolean paused = false;

    boolean softDropping = false;
    int normalDelay = 500;
    int softDropDelay = 50;

    int score = 0;
    int level = 1;
    int linesClearedTotal = 0;

    Random rand = new Random();

    JButton startButton;
    JButton replayButton;

    // --- Ready / Go 演出用 ---
    boolean showReady = false;
    boolean showGo = false;
    long messageStartTime = 0;

    public TetrisPanel() {
        setPreferredSize(new Dimension(COLS * BLOCK + 180, ROWS * BLOCK + OFFSET_Y));
        setBackground(Color.BLACK);
        setLayout(null);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(normalDelay, this);

        startButton = new JButton("START");
        startButton.setBounds(110, 260, 120, 40);
        startButton.addActionListener(e -> startGame());
        add(startButton);

        replayButton = new JButton("Replay");
        replayButton.setBounds(COLS * BLOCK + 30, ROWS * BLOCK - 30, 120, 30);
        replayButton.addActionListener(e -> startGame());
        replayButton.setVisible(false);
        add(replayButton);

        nextPiece();
    }

    void startGame() {
        gameBoard.clear();

        score = 0;
        level = 1;
        linesClearedTotal = 0;

        running = false;     // ← 演出が終わるまで false
        gameOver = false;
        landingAnim = false;
        paused = false;
        softDropping = false;

        timer.setDelay(normalDelay);

        startButton.setVisible(false);
        replayButton.setVisible(false);

        spawnPiece();

        // --- Ready → Go 演出開始 ---
        showReady = true;
        showGo = false;
        messageStartTime = System.currentTimeMillis();

        timer.start();
        requestFocusInWindow();
        repaint();
    }

    void nextPiece() {
        int i = rand.nextInt(Tetromino.SHAPES.length);
        nextBlock = Tetromino.SHAPES[i];
        nextColor = Tetromino.COLORS[i];
    }

    void spawnPiece() {
        int startX = (COLS - nextBlock[0].length) / 2;
        int startY = 0;
        currentPiece = new Piece(nextBlock, nextColor, startX, startY);

        spinActive = false;
        nextPiece();

        if (!currentPiece.canMove(gameBoard, currentPiece.x, currentPiece.y)) {
            gameOver = true;
            running = false;
            timer.stop();
            replayButton.setVisible(true);
        }
    }

    void fixPiece() {
        gameBoard.placeBlock(currentPiece.x, currentPiece.y, currentPiece.shape, currentPiece.color);

        int cleared = gameBoard.clearLines();
        score += cleared * 100;
        linesClearedTotal += cleared;
        level = Math.min(10, linesClearedTotal / 10 + 1);

        timer.setDelay(Math.max(100, normalDelay - (level - 1) * 40));

        spawnPiece();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        long now = System.currentTimeMillis();

        // --- Ready? 表示中 ---
        if (showReady) {
            if (now - messageStartTime >= 500) {
                showReady = false;
                showGo = true;
                messageStartTime = now;
            }
            repaint();
            return;
        }

        // --- Go!! 表示中 ---
        if (showGo) {
            if (now - messageStartTime >= 500) {
                showGo = false;
                running = true;  // ← ここでゲーム開始
            }
            repaint();
            return;
        }

        // --- 通常ゲーム ---
        if (!running || landingAnim || paused) return;

        if (currentPiece.canMove(gameBoard, currentPiece.x, currentPiece.y + 1)) {
            currentPiece.y++;
            spinActive = false;
        } else {
            if (!spinActive) {
                spinActive = true;
                spinStartTime = now;
                lastSpinTime = now;
            } else if (now - lastSpinTime > 500 || now - spinStartTime > 5000) {
                fixPiece();
                spinActive = false;
            }
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_S) {
            paused = !paused;
            repaint();
            return;
        }

        // 演出中は操作禁止
        if (showReady || showGo) return;

        if (!running || landingAnim || paused || currentPiece == null) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                currentPiece.moveIfPossible(gameBoard, -1, 0);
                break;

            case KeyEvent.VK_RIGHT:
                currentPiece.moveIfPossible(gameBoard, 1, 0);
                break;

            case KeyEvent.VK_UP: // ハードドロップ
                while (currentPiece.canMove(gameBoard, currentPiece.x, currentPiece.y + 1)) {
                    currentPiece.y++;
                }
                fixPiece();
                break;

            case KeyEvent.VK_SPACE: // 回転
                int[][] before = currentPiece.shape;
                if (rotateRightOrLeft()) {
                    lastSpinTime = System.currentTimeMillis();
                } else {
                    currentPiece.shape = before;
                }
                break;

            case KeyEvent.VK_DOWN: // ソフトドロップ
                softDropping = true;
                timer.setDelay(softDropDelay);
                break;
        }
        repaint();
    }

    private boolean rotateRightOrLeft() {
        int[][] original = currentPiece.shape;
        int[][] right = rotateRight(original);
        if (currentPieceCanUseShape(right)) {
            currentPiece.shape = right;
            return true;
        }
        int[][] left = rotateLeft(original);
        if (currentPieceCanUseShape(left)) {
            currentPiece.shape = left;
            return true;
        }
        return false;
    }

    private boolean currentPieceCanUseShape(int[][] testShape) {
        for (int r = 0; r < testShape.length; r++) {
            for (int c = 0; c < testShape[0].length; c++) {
                if (testShape[r][c] == 1) {
                    int nx = currentPiece.x + c;
                    int ny = currentPiece.y + r;
                    if (nx < 0 || nx >= COLS || ny >= ROWS) return false;
                    if (ny >= 0 && gameBoard.isOccupied(nx, ny)) return false;
                }
            }
        }
        return true;
    }

    private int[][] rotateRight(int[][] src) {
        int h = src.length, w = src[0].length;
        int[][] dst = new int[w][h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                dst[x][h - 1 - y] = src[y][x];
        return dst;
    }

    private int[][] rotateLeft(int[][] src) {
        int[][] t = src;
        for (int i = 0; i < 3; i++) t = rotateRight(t);
        return t;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            softDropping = false;
            timer.setDelay(normalDelay);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // --- Ready? / Go!! 表示 ---
        if (showReady) {
            drawCenteredText(g, "Ready?");
            return;
        }
        if (showGo) {
            drawCenteredText(g, "Go!!");
            return;
        }

        if (!running && !gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("TETRIS", 110, 200);
            return;
        }

        // グリッド
        g.setColor(Color.DARK_GRAY);
        for (int y = 0; y <= ROWS; y++)
            g.drawLine(0, y * BLOCK + OFFSET_Y, COLS * BLOCK, y * BLOCK + OFFSET_Y);
        for (int x = 0; x <= COLS; x++)
            g.drawLine(x * BLOCK, OFFSET_Y, x * BLOCK, ROWS * BLOCK + OFFSET_Y);

        // 固定ブロック
        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLS; x++)
                if (gameBoard.board[y][x] != null) {
                    g.setColor(gameBoard.board[y][x]);
                    g.fillRect(x * BLOCK, y * BLOCK + OFFSET_Y, BLOCK, BLOCK);
                    g.setColor(Color.BLACK);
                    g.drawRect(x * BLOCK, y * BLOCK + OFFSET_Y, BLOCK, BLOCK);
                }

        // ゴースト
        if (running && !paused && currentPiece != null) {
            int ghostY = currentPiece.getGhostY(gameBoard);
            g.setColor(new Color(
                    currentPiece.color.getRed(),
                    currentPiece.color.getGreen(),
                    currentPiece.color.getBlue(),
                    60
            ));
            for (int r = 0; r < currentPiece.shape.length; r++) {
                for (int c = 0; c < currentPiece.shape[0].length; c++) {
                    if (currentPiece.shape[r][c] == 1) {
                        int dx = (currentPiece.x + c) * BLOCK;
                        int dy = (ghostY + r) * BLOCK + OFFSET_Y;
                        g.fillRect(dx, dy, BLOCK, BLOCK);
                    }
                }
            }
        }

        // 現在ミノ
        if (running && currentPiece != null) {
            g.setColor(currentPiece.color);
            for (int r = 0; r < currentPiece.shape.length; r++) {
                for (int c = 0; c < currentPiece.shape[0].length; c++) {
                    if (currentPiece.shape[r][c] == 1) {
                        int dx = (currentPiece.x + c) * BLOCK;
                        int dy = (currentPiece.y + r) * BLOCK + OFFSET_Y;
                        g.fillRect(dx, dy, BLOCK, BLOCK);
                        g.setColor(Color.BLACK);
                        g.drawRect(dx, dy, BLOCK, BLOCK);
                        g.setColor(currentPiece.color);
                    }
                }
            }
        }

        // NEXT
        g.setColor(Color.WHITE);
        g.drawString("NEXT", COLS * BLOCK + 30, 30);
        g.setColor(nextColor);
        if (nextBlock != null) {
            for (int y = 0; y < nextBlock.length; y++)
                for (int x = 0; x < nextBlock[0].length; x++)
                    if (nextBlock[y][x] == 1)
                        g.fillRect(COLS * BLOCK + 30 + x * BLOCK, 40 + y * BLOCK, BLOCK, BLOCK);
        }

        // スコア
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, COLS * BLOCK + 30, 160);
        g.drawString("Level: " + level, COLS * BLOCK + 30, 180);

        // GAME OVER
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("GAME OVER", 60, 300);
        }

        // HOLD 表示
        if (paused) {
            drawCenteredText(g, "HOLD");
        }
    }

    private void drawCenteredText(Graphics g, String msg) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int x = (COLS * BLOCK - fm.stringWidth(msg)) / 2;
        int y = (ROWS * BLOCK + OFFSET_Y) / 2;
        g.drawString(msg, x, y);
    }
}