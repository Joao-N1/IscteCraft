package jogo.system;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SaveManager {

    private static final String SAVE_DIR = "saves/";

    public static void saveGame(String saveName, GameSaveData data) {
        // Executar numa nova Thread para o jogo não travar (lag) enquanto grava
        new Thread(() -> {
            try {
                if (!Files.exists(Paths.get(SAVE_DIR))) {
                    Files.createDirectories(Paths.get(SAVE_DIR));
                }

                String path = SAVE_DIR + saveName + ".dat";
                FileOutputStream fos = new FileOutputStream(path);
                // --- MELHORIA: GZIP para comprimir o ficheiro ---
                GZIPOutputStream gzip = new GZIPOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(gzip);

                oos.writeObject(data);

                oos.close();
                gzip.close();
                fos.close();

                System.out.println("Jogo salvo com sucesso em: " + path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static GameSaveData loadGame(String saveName) {
        try {
            String path = SAVE_DIR + saveName + ".dat";
            FileInputStream fis = new FileInputStream(path);
            // --- MELHORIA: Ler com GZIP ---
            GZIPInputStream gzip = new GZIPInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(gzip);

            GameSaveData data = (GameSaveData) ois.readObject();

            ois.close();
            gzip.close();
            fis.close();

            return data;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao carregar save: " + e.getMessage());
            return null;
        }
    }

    public static List<String> getSaveList() {
        List<String> saves = new ArrayList<>();
        File folder = new File(SAVE_DIR);
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile() && file.getName().endsWith(".dat")) {
                        saves.add(file.getName().replace(".dat", ""));
                    }
                }
            }
        }
        Collections.sort(saves); // Ordenar alfabeticamente
        return saves;
    }

    // --- NOVO: Gerar nome único ---
    public static String generateUniqueSaveName() {
        int id = 1;
        while (true) {
            String potentialName = "mundo_" + id;
            File f = new File(SAVE_DIR + potentialName + ".dat");
            if (!f.exists()) {
                return potentialName;
            }
            id++;
        }
    }
}