package org.mult.daap;

import java.io.File;

public class FileUtils {
    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    static void deleteIfExists(File file) {
        if (file != null) {
            if (file.exists() == true) {
                file.delete();
            }
        }
    }
}
