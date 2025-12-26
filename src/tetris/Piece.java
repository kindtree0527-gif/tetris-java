package tetris;

import java.awt.Color;

public class Piece {

    int[][] shape;
    Color color;
    int x;
    int y;

    public Piece(int[][] shape, Color color, int startX, int startY) {
        // shape をコピーしておく（安全のため）
        int h = shape.length;
        int w = shape[0].length;
        this.shape = new int[h][w];
        for (int r = 0; r < h; r++) {
            System.arraycopy(shape[r], 0, this.shape[r], 0, w);
        }
        this.color = color;
        this.x = startX;
        this.y = startY;
    }

    public boolean canMove(GameBoard board, int newX, int newY) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] == 1) {
                    int nx = newX + c;
                    int ny = newY + r;
                    if (nx < 0 || nx >= GameBoard.COLS || ny >= GameBoard.ROWS) return false;
                    if (ny >= 0 && board.isOccupied(nx, ny)) return false;
                }
            }
        }
        return true;
    }

    public void moveIfPossible(GameBoard board, int dx, int dy) {
        if (canMove(board, x + dx, y + dy)) {
            x += dx;
            y += dy;
        }
    }

    private int[][] rotateRight(int[][] src) {
        int h = src.length, w = src[0].length;
        int[][] dst = new int[w][h];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                dst[c][h - 1 - r] = src[r][c];
            }
        }
        return dst;
    }

    private int[][] rotateLeft(int[][] src) {
        int[][] t = src;
        for (int i = 0; i < 3; i++) {
            t = rotateRight(t);
        }
        return t;
    }

    public boolean rotateWithKick(GameBoard board, boolean preferRight) {
        int[][] r = rotateRight(shape);
        if (canMove(board, x, y)) {
            // no-op
        }
        // まず右回転
        if (canMove(board, x, y, r)) {
            shape = r;
            return true;
        }
        // 右回転が無理なら左回転を試す
        int[][] l = rotateLeft(shape);
        if (canMove(board, x, y, l)) {
            shape = l;
            return true;
        }
        return false;
    }

    private boolean canMove(GameBoard board, int newX, int newY, int[][] testShape) {
        for (int r = 0; r < testShape.length; r++) {
            for (int c = 0; c < testShape[0].length; c++) {
                if (testShape[r][c] == 1) {
                    int nx = newX + c;
                    int ny = newY + r;
                    if (nx < 0 || nx >= GameBoard.COLS || ny >= GameBoard.ROWS) return false;
                    if (ny >= 0 && board.isOccupied(nx, ny)) return false;
                }
            }
        }
        return true;
    }

    public int getGhostY(GameBoard board) {
        int gy = y;
        while (canMove(board, x, gy + 1)) {
            gy++;
        }
        return gy;
    }
}