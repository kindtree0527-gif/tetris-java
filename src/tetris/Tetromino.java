package tetris;

import java.awt.Color;

public class Tetromino {

    // 1: ブロックあり, 0: なし
    public static final int[][][] SHAPES = {
            // I
            {
                    {1, 1, 1, 1}
            },
            // O
            {
                    {1, 1},
                    {1, 1}
            },
            // T
            {
                    {0, 1, 0},
                    {1, 1, 1}
            },
            // S
            {
                    {0, 1, 1},
                    {1, 1, 0}
            },
            // Z
            {
                    {1, 1, 0},
                    {0, 1, 1}
            },
            // J
            {
                    {1, 0, 0},
                    {1, 1, 1}
            },
            // L
            {
                    {0, 0, 1},
                    {1, 1, 1}
            }
    };

    public static final Color[] COLORS = {
            Color.CYAN,    // I
            Color.YELLOW,  // O
            Color.MAGENTA, // T
            Color.GREEN,   // S
            Color.RED,     // Z
            Color.BLUE,    // J
            new Color(255, 140, 0)  // L
    };
}