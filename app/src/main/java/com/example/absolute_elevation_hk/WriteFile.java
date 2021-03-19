package com.example.absolute_elevation_hk;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class WriteFile {
    static public void writeTxtToFiles(String dirName, String fileName, String content) {
        boolean Record=true;
        if (content==null){
            Record = false;
        }
        if(Record) {
            String strContent = content ;
            File parent_path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
            if (TextUtils.isEmpty(fileName)) {
                fileName = String.valueOf(System.currentTimeMillis());
            }
            String dirPath = parent_path.getAbsolutePath() + "/" + dirName;
            try {
                File dirFile = new File(dirPath);
                if (!dirFile.exists()) {
                    dirFile.mkdir();
                }
                String strFilePath = dirPath + "/" + fileName;

                File mFile = new File(strFilePath);
                if (!mFile.exists()) {
                    mFile.createNewFile();
                } else {

                }

                RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
                raf.seek(mFile.length());
                raf.write(strContent.getBytes());
                raf.close();
            } catch (Exception e) {
                Log.e("TestFile", "Error on write File:" + e);
            }
        }
    }

    public static void rewriteTxtToFiles(String dirName, String fileName, String content) {
        boolean Record=true;
        if (content==null){
            Record = false;
        }
        if(Record) {
            String strContent = content ;
            File parent_path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
            if (TextUtils.isEmpty(fileName)) {
                fileName = String.valueOf(System.currentTimeMillis());
            }
            String dirPath = parent_path.getAbsolutePath() + "/" + dirName;
            try {
                File dirFile = new File(dirPath);
                if (!dirFile.exists()) {
                    dirFile.mkdir();
                }
                String strFilePath = dirPath + "/" + fileName;

                File mFile = new File(strFilePath);
                if (!mFile.exists()) {
                    mFile.createNewFile();
                } else {

                }

                RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
                raf.setLength(0);
                raf.write(strContent.getBytes());
                raf.close();
            } catch (Exception e) {
                Log.e("TestFile", "Error on write File:" + e);
            }
        }
    }
}
