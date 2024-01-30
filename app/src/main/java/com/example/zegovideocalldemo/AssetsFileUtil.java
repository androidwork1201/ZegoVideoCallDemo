package com.example.zegovideocalldemo;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssetsFileUtil {

    public static List<String> getAllAssetsFile(Context context) {
        List<String> fileList = new ArrayList<>();
        AssetManager assetManager = context.getAssets();

        try {
            String[] files = assetManager.list("");

            if (files != null) {
                getAllFilesInAssetsRecursive("", files, assetManager, fileList);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList;
    }

    private static void getAllFilesInAssetsRecursive(String path, String[] files, AssetManager assetManager, List<String> fileList) {
        for (String file : files) {
            try {
                // 构建文件或文件夹的完整路径
                String fullPath = path.isEmpty() ? file : path + "/" + file;

                // 判断是文件还是文件夹
                if (assetManager.list(fullPath).length == 0) {
                    // 是文件，添加到列表中
                    fileList.add(fullPath);
                } else {
                    // 是文件夹，递归遍历子文件夹
                    String[] subFiles = assetManager.list(fullPath);
                    getAllFilesInAssetsRecursive(fullPath, subFiles, assetManager, fileList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyFileFromAssets(Context context, String assetsFilePath, String targetFileFullPath) {

        try {
            if(assetsFilePath.endsWith(File.separator))
            {
                assetsFilePath = assetsFilePath.substring(0,assetsFilePath.length()-1);
            }
            String fileNames[] = context.getAssets().list(assetsFilePath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {
                File file = new File(targetFileFullPath);
                file.mkdirs();
                for (String fileName : fileNames) {
                    copyFileFromAssets(context, assetsFilePath + File.separator + fileName, targetFileFullPath + File.separator + fileName);
                }
            } else {//如果是文件

                File file = new File(targetFileFullPath);
                file.getParentFile().mkdir();

                InputStream is = context.getAssets().open(assetsFilePath);

                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();

            }

        } catch (Exception e) {
            Log.d("Tag", "copyFileFromAssets " + "IOException-" + e.getMessage());
        }
    }

}
