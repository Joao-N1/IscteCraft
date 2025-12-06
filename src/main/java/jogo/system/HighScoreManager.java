package jogo.system;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class HighScoreManager {
    private static final String SCORE_FILE = "highscores.dat";
    private static final int MAX_SCORES = 5; // Top 5

    // Classe interna para representar uma pontuação
    public static class ScoreEntry implements Serializable, Comparable<ScoreEntry> {
        public String playerName;
        public float timeTaken; // Tempo em segundos (menor é melhor)

        public ScoreEntry(String name, float time) {
            this.playerName = name;
            this.timeTaken = time;
        }

        @Override
        public int compareTo(ScoreEntry other) {
            // Ordem decrescente para a PriorityQueue (para o maior sair primeiro quando limitamos o tamanho)
            // OU Ordem crescente para display.
            // Para manter os 5 MELHORES (menor tempo), a PQ deve remover o MAIOR tempo.
            return Float.compare(other.timeTaken, this.timeTaken);
        }

        @Override
        public String toString() {
            return String.format("%s - %.2fs", playerName, timeTaken);
        }
    }

    private PriorityQueue<ScoreEntry> scores;

    public HighScoreManager() {
        // Inicializar a fila
        scores = new PriorityQueue<>();
        loadScores();
    }

    public void addScore(String name, float time) {
        ScoreEntry newScore = new ScoreEntry(name, time);
        scores.add(newScore);

        // Se passarmos o limite, removemos o pior (o que demorou mais tempo)
        if (scores.size() > MAX_SCORES) {
            scores.poll(); // Remove a cabeça da fila (definido pelo compareTo)
        }

        saveScores();
    }

    public List<ScoreEntry> getTopScores() {
        // Converter a Fila numa Lista ordenada para exibir
        List<ScoreEntry> list = new ArrayList<>(scores);
        // Ordenar por menor tempo primeiro (o contrário do compareTo da PQ)
        list.sort((s1, s2) -> Float.compare(s1.timeTaken, s2.timeTaken));
        return list;
    }

    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
            oos.writeObject(new ArrayList<>(scores)); // Serializar como lista é mais simples
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadScores() {
        File f = new File(SCORE_FILE);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            List<ScoreEntry> list = (List<ScoreEntry>) ois.readObject();
            scores.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}