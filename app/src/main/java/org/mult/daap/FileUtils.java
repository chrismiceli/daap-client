package org.mult.daap;

import java.io.File;

public class FileUtils {
    static public void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }

    static void deleteIfExists(File file) {
        if (file != null) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
