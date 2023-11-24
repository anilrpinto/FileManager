package com.anilrpinto.filemanager.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class FileManager {

    private static final String DUP_INDX_FORMAT = "-%s-dup%02d.";

    private static final String DUPLICATES = "duplicates";

    private static HashMap<Long, List<File>> grouped = new HashMap<>();

    private static long dirCount = 0;

    private static long fileCount = 0;

    private static long delete = 0;

    private static BigInteger size = BigInteger.valueOf(0);

    public static void sort(Set<String> dirs, boolean recurse, boolean imagesOnly, ProgressListener listener) throws Exception {

        File main = null;

        for (String path : dirs) {

            File dir = Paths.get(path).toFile();

            Log.d("", dir.getAbsolutePath());

            if (main == null)
                main = dir;

            File[] files = getFiles(dir, recurse, imagesOnly);

            //count += files.length;

            size = size.add(process(files, recurse, imagesOnly, size, listener));
        }

        listener.update("dir-count", dirCount);
        listener.update("files-count", fileCount);
        listener.update("files-size", asSize(size));

        System.out.println("Sorted size:" + grouped.size());

        move(main, grouped, listener);

        listener.update("done", null);
    }

    private static BigInteger process(File[] files, boolean recurse, boolean imagesOnly, BigInteger size, ProgressListener listener) {
        for (File f : files) {

            if (f.isDirectory()) {
                listener.update("dir-count", ++dirCount);
                size = process(getFiles(f, recurse, imagesOnly), recurse, imagesOnly, size, listener);
            } else {

                listener.update("files-count", ++fileCount);

                long key = f.length();

                size = size.add(BigInteger.valueOf(key));
                listener.update("files-size", asSize(size));

                List<File> names = grouped.get(key);

                if (names == null)
                    grouped.put(key, names = new ArrayList<>());

                names.add(f);
            }
        }

        return size;
    }

    private static File[] getFiles(File dir, boolean recurse, boolean imagesOnly) {
        return dir.listFiles(new CustomFileFilter(recurse, imagesOnly));
    }

    private static String asSize(BigInteger size) {
        return String.format("%s MB", size.divide(BigInteger.valueOf(1024 * 1024)));
    }

    private static void move(File dir, HashMap<Long, List<File>> grouped, ProgressListener listener) throws Exception {

        final int[] count = {0};

        listener.update("start", grouped.size());

        Path dupsDir = Paths.get(dir.getAbsolutePath()).resolve(DUPLICATES);
        Files.createDirectories(dupsDir);

        Iterator<Long> keys = grouped.keySet().iterator();

        BigInteger[] dupInfo = {BigInteger.valueOf(0), BigInteger.valueOf(0)};
        listener.update("dup-count", dupInfo[0]);
        listener.update("dup-size", asSize(dupInfo[1]));

        while (keys.hasNext()) {

            listener.update("progress", ++count[0]);

            Long k = keys.next();
            List<File> files = grouped.get(k);

            if (files.size() > 1) {
                dupInfo = compare(files, dupsDir, dupInfo, listener);
            } else
                keys.remove();

        }

        //listener.update("dup-count", dupInfo[0]);
        //listener.update("dup-size", asSize(dupInfo[1]));
        System.out.println("Grouped size:" + grouped.size());
    }

    private static BigInteger[] compare(List<File> files, Path dupsDir, BigInteger[] dupInfo, ProgressListener listener) throws Exception {
        File main = null;
        int indx = 0;

        for (File f : files) {

            if (main == null) {
                main = f;
                continue;
            }

            Thread.sleep(15);
            listener.update("current", f.getName());

            if (match(main, f)) {
                Files.move(Paths.get(f.getAbsolutePath()), dupsDir.resolve(main.getName().replace(".", String.format(DUP_INDX_FORMAT, main.getParentFile().getName(), ++indx))), StandardCopyOption.REPLACE_EXISTING);
                dupInfo[0] = dupInfo[0].add(BigInteger.valueOf(1));
                listener.update("dup-count", dupInfo[0]);
                dupInfo[1] = dupInfo[1].add(BigInteger.valueOf(main.length()));
                listener.update("dup-size", asSize(dupInfo[1]));
            }
        }

        return dupInfo;
    }

    private static boolean match(File left, File right) throws Exception {

        System.out.println(left.getAbsolutePath());

        try (InputStream l = new BufferedInputStream(new FileInputStream(left));
             InputStream r = new BufferedInputStream(new FileInputStream(right))) {

            int b;
            do {
                b = l.read();
                if (b != r.read())
                    return false;

            } while (b >= 0);
        }

        return true;
    }

    public static void deleteDirs(List<String> dirs, ProgressListener listener) {
        dirCount = 0;
        delete = 0;

        for (String path : dirs) {
            File dir = Paths.get(path).toFile();
            processForDelete(false, dir, dir.listFiles(), listener);
        }

        listener.update("done", null);
    }

    private static void processForDelete(boolean child, File dir, File[] items, ProgressListener listener) {

        for (File i : items) {
            listener.update("dir-count", ++dirCount);
            if (i.isDirectory()) {
                File[] files = i.listFiles();
                if (files.length == 0) {
                    i.delete();
                    listener.update("delete-count", ++delete);
                    listener.update("current", i.getAbsolutePath());
                } else
                    processForDelete(true, i, files, listener);
            }
        }

        if (child && dir.list().length == 0) {
            dir.delete();
            listener.update("delete-count", ++delete);
        }
    }

    public interface ProgressListener {

        void update (String code, Object data);

    }

    private static class CustomFileFilter implements FileFilter {

        private Function<File, Boolean> code;

        private static final List<String> IMAGE_TYPES = Arrays.asList(new String[] {"jpg", "png", "gif"});

       public CustomFileFilter(boolean recurse, boolean imagesOnly) {
           if (recurse) {
               if (imagesOnly)
                   code = (f) -> f.isDirectory() && !DUPLICATES.equals(f.getName()) || IMAGE_TYPES.contains(f.getName().substring(f.getName().lastIndexOf(".") + 1));
               else
                   code = (f) -> f.isDirectory() && !DUPLICATES.equals(f.getName()) || f.isFile();
           } else {
               if (imagesOnly)
                   code = (f) -> f.isFile() && IMAGE_TYPES.contains(f.getName().substring(f.getName().lastIndexOf(".") + 1));
               else
                   code = (f) -> f.isFile();
           }
       }

        @Override
        public boolean accept(File f) {
           return code.apply(f);
        }
    }

}
