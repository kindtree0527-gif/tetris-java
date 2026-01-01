package tetris;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScoreManager {
    private static final String FILE_NAME = ".tetris_scores.txt";

    private ScoreManager() {}

    private static Path scoreFile() {
        return Paths.get(System.getProperty("user.home"), FILE_NAME);
    }

    public static synchronized List<Integer> loadScores() {
        Path path = scoreFile();
        if (!Files.exists(path)) return new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<Integer> scores = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    scores.add(Integer.parseInt(line));
                } catch (NumberFormatException ignore) {}
            }
            scores.sort(Collections.reverseOrder());
            return scores;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static synchronized int getBestScore() {
        List<Integer> scores = loadScores();
        return scores.isEmpty() ? 0 : scores.get(0);
    }

    public static synchronized void addScore(int score) {
        List<Integer> scores = loadScores();
        scores.add(score);
        scores.sort(Collections.reverseOrder());

        // 肥大化防止：上位100件まで保存
        if (scores.size() > 100) {
            scores = new ArrayList<>(scores.subList(0, 100));
        }

        saveScores(scores);
    }

    public static synchronized List<Integer> getTopScores(int topN) {
        List<Integer> scores = loadScores();
        int n = Math.min(topN, scores.size());
        return new ArrayList<>(scores.subList(0, n));
    }

    private static void saveScores(List<Integer> scores) {
        Path path = scoreFile();
        List<String> lines = new ArrayList<>();
        for (Integer s : scores) lines.add(String.valueOf(s));
        try {
            Files.write(path, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignore) {}
    }
}
