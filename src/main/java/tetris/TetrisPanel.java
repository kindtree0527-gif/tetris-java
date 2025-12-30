package tetris;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

public class TetrisPanel extends JPanel implements ActionListener, KeyListener {
    static final int COLS = 10;
    static final int ROWS = 20;
    static final int BLOCK = 30;
    static final int OFFSET_Y = 40;

    GameBoard gameBoard = new GameBoard();
    Timer timer;
    Piece currentPiece;
    int[][] nextBlock;
    Color nextColor;

    boolean running = false;
    boolean gameOver = false;

    boolean spinActive = false;
    long spinStartTime;
    long lastSpinTime;

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

    boolean showReady = false;
    boolean showGo = false;
    long messageStartTime = 0L;

    static final int GAME_DURATION_SEC = 180;
    long playStartTimeMs = 0L;
    int remainingSeconds = 180;
    boolean finished = false;

    // ★BGM用（プレイ中ループ）
    private Clip bgmClip;

    public TetrisPanel() {
        this.setPreferredSize(new Dimension(480, 640));
        this.setBackground(Color.BLACK);
        this.setLayout((LayoutManager) null);
        this.setFocusable(true);
        this.addKeyListener(this);
        this.timer = new Timer(this.normalDelay, this);

        // ★BGMを読み込み（resources/sound/bgm.wav）
        this.bgmClip = loadClip("/sound/bgm.wav");

        this.startButton = new JButton("START");
        this.startButton.setBounds(110, 260, 120, 40);
        this.startButton.addActionListener((e) -> {
            this.playStartSound();
            this.startGame();
        });
        this.add(this.startButton);

        this.replayButton = new JButton("Replay");
        this.replayButton.setBounds(330, 570, 120, 30);
        this.replayButton.addActionListener((e) -> {
            this.playStartSound();
            this.startGame();
        });
        this.replayButton.setVisible(false);
        this.add(this.replayButton);

        this.nextPiece();
    }

    void startGame() {
        // ★再スタート時も含めてBGMを止めてから開始（安全策）
        stopBgm();

        this.gameBoard.clear();
        this.score = 0;
        this.level = 1;
        this.linesClearedTotal = 0;
        this.running = false;
        this.gameOver = false;
        this.paused = false;
        this.softDropping = false;
        this.finished = false;
        this.remainingSeconds = GAME_DURATION_SEC;
        this.playStartTimeMs = 0L;

        this.timer.setDelay(this.normalDelay);
        this.startButton.setVisible(false);
        this.replayButton.setVisible(false);

        this.spawnPiece();

        this.showReady = true;
        this.showGo = false;
        this.messageStartTime = System.currentTimeMillis();
        this.timer.start();
        this.requestFocusInWindow();
        this.repaint();
    }

    // Clipを読み込んで保持する（BGM用）
    private Clip loadClip(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.out.println("音声ファイルが見つかりません: " + path);
                return null;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // ★BGM開始（ループ）
    private void startBgmLoop() {
        if (bgmClip == null) return;
        if (bgmClip.isRunning()) return;

        bgmClip.stop();
        bgmClip.setFramePosition(0);
        bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        bgmClip.start();
    }

    // ★BGM停止
    private void stopBgm() {
        if (bgmClip == null) return;
        if (bgmClip.isRunning()) {
            bgmClip.stop();
        }
        bgmClip.setFramePosition(0);
    }

    private void playStartSound() {
        try {
            URL soundURL = this.getClass().getResource("/sound/start.wav");
            if (soundURL == null) {
                System.out.println("start.wav が見つかりません: /sound/start.wav");
                return;
            }
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
    }

    // ライン消去（ミノ消滅）効果音
    private void playLineClearSound() {
        try {
            URL soundURL = this.getClass().getResource("/sound/line_clear.wav");
            if (soundURL == null) {
                System.out.println("line_clear.wav が見つかりません: /sound/line_clear.wav");
                return;
            }
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
    }

    // ★追加：テトリス（4ライン以上）専用効果音
    private void playTetrisSound() {
        try {
            URL soundURL = this.getClass().getResource("/sound/tetris.wav");
            if (soundURL == null) {
                System.out.println("tetris.wav が見つかりません: /sound/tetris.wav");
                return;
            }
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
    }

    private void playGameOverSound() {
        try {
            URL soundURL = this.getClass().getResource("/sound/gameover.wav");
            if (soundURL == null) {
                System.out.println("gameover.wav が見つかりません: /sound/gameover.wav");
                return;
            }
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
    }

    private void playFinishSound() {
        try {
            URL soundURL = this.getClass().getResource("/sound/finish.wav");
            if (soundURL == null) {
                System.out.println("finish.wav が見つかりません: /sound/finish.wav");
                return;
            }
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
    }

    void nextPiece() {
        int i = this.rand.nextInt(Tetromino.SHAPES.length);
        this.nextBlock = Tetromino.SHAPES[i];
        this.nextColor = Tetromino.COLORS[i];
    }

    void spawnPiece() {
        this.currentPiece = new Piece(this.nextBlock, this.nextColor, 3, 0);
        this.spinActive = false;
        this.nextPiece();

        if (!this.currentPiece.canMove(this.gameBoard, this.currentPiece.x, this.currentPiece.y)) {
            this.gameOver = true;

            // ★ゲームオーバーでBGM停止
            stopBgm();

            this.playGameOverSound();
            this.running = false;
            this.timer.stop();
            this.replayButton.setVisible(true);
        }
    }

    void fixPiece() {
        this.gameBoard.placeBlock(this.currentPiece.x, this.currentPiece.y, this.currentPiece.shape, this.currentPiece.color);

        int cleared = this.gameBoard.clearLines();

        // ★修正：テトリス(4ライン以上)の時だけ専用SE、それ以外は通常SE
        if (cleared >= 4) {
            this.playTetrisSound();
        } else if (cleared > 0) {
            this.playLineClearSound();
        }

        this.score += cleared * 100;
        this.linesClearedTotal += cleared;

        int newLevel = 1 + this.linesClearedTotal / 10;
        if (newLevel != this.level) {
            this.level = newLevel;
            this.normalDelay = Math.max(100, 500 - (this.level - 1) * 40);
            this.timer.setDelay(this.normalDelay);
        }

        this.spawnPiece();
    }

    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();

        if (this.showReady) {
            if (now - this.messageStartTime >= 500L) {
                this.showReady = false;
                this.showGo = true;
                this.messageStartTime = now;
            }
            this.repaint();
            return;
        }

        if (this.showGo) {
            if (now - this.messageStartTime >= 500L) {
                this.showGo = false;

                // ★Go!! が終わってゲーム開始した瞬間
                this.running = true;
                this.finished = false;
                this.playStartTimeMs = now;
                this.remainingSeconds = GAME_DURATION_SEC;

                // ★BGM開始（プレイ中ずっと）
                startBgmLoop();
            }
            this.repaint();
            return;
        }

        if (this.running && !this.paused) {
            if (this.playStartTimeMs > 0L) {
                long elapsedSec = (now - this.playStartTimeMs) / 1000L;
                this.remainingSeconds = (int) Math.max(0L, GAME_DURATION_SEC - elapsedSec);
                if (this.remainingSeconds <= 0) {
                    this.finishGame();
                    this.repaint();
                    return;
                }
            }

            if (this.currentPiece.canMove(this.gameBoard, this.currentPiece.x, this.currentPiece.y + 1)) {
                ++this.currentPiece.y;
                this.spinActive = false;
            } else if (!this.spinActive) {
                this.spinActive = true;
                this.spinStartTime = now;
                this.lastSpinTime = now;
            } else if (now - this.lastSpinTime > 500L || now - this.spinStartTime > 5000L) {
                this.fixPiece();
                this.spinActive = false;
            }

            this.repaint();
        }
    }

    private void finishGame() {
        this.finished = true;
        this.running = false;
        this.paused = false;
        this.softDropping = false;

        // ★FINISHでBGM停止
        stopBgm();

        this.playFinishSound();
        this.timer.stop();
        this.replayButton.setVisible(true);
    }

    public void keyPressed(KeyEvent e) {
        if (!this.showReady && !this.showGo && !this.finished && !this.gameOver) {
            int key = e.getKeyCode();

            // ★変更：SでHOLD（paused）になったらBGM停止、解除で再開
            if (key == KeyEvent.VK_S) {
                this.paused = !this.paused;

                if (this.paused) {
                    // HOLD中はBGMを消す（停止）
                    stopBgm();
                } else {
                    // HOLD解除でプレイ中ならBGM再開
                    if (this.running) {
                        startBgmLoop();
                    }
                }

                this.repaint();
            } else if (this.running && !this.paused) {
                if (key == KeyEvent.VK_LEFT) {
                    if (this.currentPiece.canMove(this.gameBoard, this.currentPiece.x - 1, this.currentPiece.y)) {
                        --this.currentPiece.x;
                    }
                    if (this.spinActive) this.lastSpinTime = System.currentTimeMillis();
                } else if (key == KeyEvent.VK_RIGHT) {
                    if (this.currentPiece.canMove(this.gameBoard, this.currentPiece.x + 1, this.currentPiece.y)) {
                        ++this.currentPiece.x;
                    }
                    if (this.spinActive) this.lastSpinTime = System.currentTimeMillis();
                } else if (key == KeyEvent.VK_DOWN) {
                    this.softDropping = true;
                    this.timer.setDelay(this.softDropDelay);
                } else if (key == KeyEvent.VK_SPACE) {
                    boolean rotated = this.currentPiece.rotateWithKick(this.gameBoard, true);
                    if (rotated && this.spinActive) this.lastSpinTime = System.currentTimeMillis();
                } else if (key == KeyEvent.VK_UP) {
                    while (this.currentPiece.canMove(this.gameBoard, this.currentPiece.x, this.currentPiece.y + 1)) {
                        ++this.currentPiece.y;
                    }
                    this.fixPiece();
                }

                this.repaint();
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            this.softDropping = false;
            this.timer.setDelay(this.normalDelay);
        }
    }

    public void keyTyped(KeyEvent e) {}

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (this.showReady) {
            this.drawCenteredText(g, "Ready?", Color.WHITE);
            return;
        }

        if (this.showGo) {
            this.drawCenteredText(g, "Go!!", Color.WHITE);
            return;
        }

        if (!this.running && !this.gameOver && !this.finished && this.startButton.isVisible()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("TETRIS", 110, 200);
            return;
        }

        g.setColor(Color.DARK_GRAY);

        for (int y = 0; y <= 20; ++y) {
            g.drawLine(0, y * 30 + 40, 300, y * 30 + 40);
        }
        for (int x = 0; x <= 10; ++x) {
            g.drawLine(x * 30, 40, x * 30, 640);
        }

        for (int y = 0; y < 20; ++y) {
            for (int x = 0; x < 10; ++x) {
                Color c = this.gameBoard.board[y][x];
                if (c != null) {
                    this.drawBlockCell(g, x, y, c, false);
                }
            }
        }

        // ===== ゴーストミノ（復活）=====
        if (this.running && !this.paused && !this.gameOver && !this.finished && this.currentPiece != null) {
            int ghostY = this.currentPiece.getGhostY(this.gameBoard);
            if (ghostY != this.currentPiece.y) {
                for (int r = 0; r < this.currentPiece.shape.length; ++r) {
                    for (int c = 0; c < this.currentPiece.shape[0].length; ++c) {
                        if (this.currentPiece.shape[r][c] == 1) {
                            int x = this.currentPiece.x + c;
                            int y = ghostY + r;
                            if (y >= 0) {
                                this.drawBlockCell(g, x, y, this.currentPiece.color, true);
                            }
                        }
                    }
                }
            }
        }

        // 現在のミノ
        if (!this.gameOver && this.currentPiece != null) {
            for (int r = 0; r < this.currentPiece.shape.length; ++r) {
                for (int c = 0; c < this.currentPiece.shape[0].length; ++c) {
                    if (this.currentPiece.shape[r][c] == 1) {
                        int x = this.currentPiece.x + c;
                        int y = this.currentPiece.y + r;
                        if (y >= 0) {
                            this.drawBlockCell(g, x, y, this.currentPiece.color, false);
                        }
                    }
                }
            }
        }

        // ===== NEXT表示（復活）=====
        g.setColor(Color.WHITE);
        g.drawString("NEXT:", 330, 30);

        if (this.nextBlock != null) {
            for (int y = 0; y < this.nextBlock.length; ++y) {
                for (int x = 0; x < this.nextBlock[0].length; ++x) {
                    if (this.nextBlock[y][x] == 1) {
                        int px = 330 + x * 30;
                        int py = 40 + y * 30;
                        g.setColor(this.nextColor);
                        g.fillRect(px, py, 30, 30);
                        g.setColor(Color.BLACK);
                        g.drawRect(px, py, 30, 30);
                    }
                }
            }
        }

        g.setColor(Color.WHITE);
        g.drawString("Score: " + this.score, 330, 160);
        g.drawString("Level: " + this.level, 330, 180);
        g.drawString("Time: " + this.formatTime(this.remainingSeconds), 330, 200);

        // GAME OVERは赤
        if (this.gameOver) {
            this.drawCenteredText(g, "GAME OVER", Color.RED);
        }
        if (this.finished) {
            this.drawCenteredText(g, "Finish", Color.WHITE);
        }
        if (this.paused) {
            this.drawCenteredText(g, "HOLD", Color.WHITE);
        }
    }

    private void drawBlockCell(Graphics g, int gridX, int gridY, Color baseColor, boolean ghost) {
        int px = gridX * 30;
        int py = gridY * 30 + 40;

        if (ghost) {
            Graphics2D g2 = (Graphics2D) g;
            Composite old = g2.getComposite();

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25F));
            g2.setColor(baseColor);
            g2.fillRect(px, py, 30, 30);
            g2.setColor(Color.BLACK);
            g2.drawRect(px, py, 30, 30);

            g2.setComposite(old);
        } else {
            g.setColor(baseColor);
            g.fillRect(px, py, 30, 30);
            g.setColor(Color.BLACK);
            g.drawRect(px, py, 30, 30);
        }
    }

    private String formatTime(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void drawCenteredText(Graphics g, String msg, Color color) {
        g.setColor(color);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int x = (300 - fm.stringWidth(msg)) / 2;
        int y = 320;
        g.drawString(msg, x, y);
    }
}
