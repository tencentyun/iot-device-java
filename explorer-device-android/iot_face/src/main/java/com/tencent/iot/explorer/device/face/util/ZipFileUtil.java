package com.tencent.iot.explorer.device.face.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipFileUtil {

    /**
     * Compress file
     * @param zipFileName the compress file name
     * @param srcPathName the subject file or directory to be compressed
     * @throws IOException
     */
    public static void zip(String zipFileName,String srcPathName) throws IOException {
        File file = new File(srcPathName);
        if(!file.exists()){
            throw new RuntimeException("Sorry,"+srcPathName+" not exists.");
        }
        FileOutputStream fos = new FileOutputStream(new File(zipFileName));
        CheckedOutputStream cos = new CheckedOutputStream(fos,new CRC32());
        ZipOutputStream out = new ZipOutputStream(cos);
        String basedir = "";
        compress(file,out,basedir);
        out.flush();
        out.close();
    }

    private static void compress(File file,ZipOutputStream out,String basedir) throws IOException{
        if(file.isDirectory()){
            compressDirectory(file,out,basedir);
        }else{
            compressFile(file,out,basedir);
        }
    }
    private static void compressDirectory(File dir,ZipOutputStream out,String basedir) throws IOException{
        if(!dir.exists()){
            return;
        }
        File []files = dir.listFiles();
        for(int i=0,len=files.length;i<len;i++){
            compress(files[i],out,basedir+dir.getName()+"/");
        }
    }
    private static void compressFile(File file,ZipOutputStream out,String basedir) throws IOException{
        if(!file.exists()){
            return;
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        ZipEntry entry = new ZipEntry(basedir+file.getName());
        out.putNextEntry(entry);
        int len = 0;
        byte []data = new byte[1024];
        while((len=bis.read(data))!=-1){
            out.write(data,0,len);
        }
        bis.close();
    }

    /**
     * uncompress file
     * @param zipFilePath file endWith '.zip' which to be uncompressed  eg:"/sdcard/face_for_reg/faces.zip"
     * @param unzipDirectory uncompress files location eg:"/sdcard/face_for_reg"
     * @throws IOException
     * @throws IOException
     */
    public static void unzip(String zipFilePath,String unzipDirectory) throws IOException, IOException{
        File file = new File(zipFilePath);
        ZipFile zipFile = new ZipFile(file);
        File unzipFile = new File(unzipDirectory);
        Enumeration zipEnum = zipFile.entries();
        InputStream in = null;
        OutputStream out = null;
        ZipEntry entry = null;
        while(zipEnum.hasMoreElements()){
            entry = (ZipEntry) zipEnum.nextElement();
            String entryName = entry.getName();
            String []names = entryName.split("\\/");
            int len = names.length;
            String path = unzipFile.getAbsolutePath();
            for(int i=0;i<len;i++){
                if(i<len-1){
                }else{
                    if(entryName.endsWith("/")){
                    }else{
                        in = zipFile.getInputStream(entry);
                        String unzipFileName = unzipFileName(entryName);
                        if (!unzipFileName.equals("")) {
                            out = new FileOutputStream(new File(unzipFile.getAbsolutePath()+"/"+unzipFileName));
                            byte []b = new byte[1024];
                            int pos = 0;
                            while((pos = in.read(b))!=-1){
                                out.write(b,0,pos);
                            }
                            in.close();
                            out.flush();
                            out.close();
                        }
                    }
                }

            }
        }
        zipFile.close();
    }

    private static String unzipFileName(String name){
        String []paths = name.split("\\/");
        if (paths.length > 0) {
            return paths[paths.length-1];
        } else {
            return "";
        }
    }

}
