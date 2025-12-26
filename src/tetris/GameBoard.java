package tetris;

import java.awt.Color;

public class GameBoard {

    public static final int COLS = 10;
    public static final int ROWS = 20;

    // [row][col]
    Color[][] board = new Color[ROWS][COLS];

    public void clear() {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                board[y][x] = null;
            }
        }
    }

    public boolean isOccupied(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return true;
        return board[y][x] != null;
    }

    public void placeBlock(int baseX, int baseY, int[][] shape, Color color) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] == 1) {
                    int x = baseX + c;
                    int y = baseY + r;
                    if (y >= 0 && y < ROWS && x >= 0 && x < COLS) {
                        board[y][x] = color;
                    }
                }
            }
        }
    }

    public int clearLines() {
        int cleared = 0;
        for (int y = 0; y < ROWS; y++) {
            boolean full = true;
            for (int x = 0; x < COLS; x++) {
                if (board[y][x] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                // 1 行詰める
                for (int yy = y; yy > 0; yy--) {
                    System.arraycopy(board[yy - 1], 0, board[yy], 0, COLS);
                }
                // 最上段を空に
                for (int x = 0; x < COLS; x++) {
                    board[0][x] = null;
                }
                cleared++;
            }
        }
        return cleared;
    }
}