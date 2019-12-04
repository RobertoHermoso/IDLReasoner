package es.us.isa.idlreasoner.util;

import java.io.*;

public class FileManager {

    public static File recreateFile(String filePath) {
        File file = new File(filePath);
        file.delete();
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static File createFileIfNotExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static void appendContentToFile(String filePath, String content) {
        File file = new File(filePath);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
            out.append(content);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedWriter openWriter(String filePath) {
        File file = new File(filePath);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static BufferedReader openReader(String filePath) {
        File file = new File(filePath);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }
}
