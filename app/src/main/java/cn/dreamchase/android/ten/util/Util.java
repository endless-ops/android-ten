package cn.dreamchase.android.ten.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import java.net.URLEncoder;
import java.util.List;

import cn.dreamchase.android.ten.net.NameValuePair;

public class Util {

    public static int getVersionCode(Context context) {
        int version = 0;
        try{
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(),0);
            version = packageInfo.versionCode;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    /**
     * 获取文件保存路径 sdcard根目录/download/文件名称
     * @param fileUrl
     * @return
     */
    public static String getSaveFilePath(String fileUrl){
        String fileName=fileUrl.substring(fileUrl.lastIndexOf("/")+1,fileUrl.length());//获取文件名称
        String newFilePath= Environment.getExternalStorageDirectory() + "/Download/"+fileName;
        return newFilePath;
    }

    /**
     * 获取文件名称
     * @param filename
     * @return
     */
    public static String getFileName(String filename){
        int start=filename.lastIndexOf("/");
//        int end=filename.lastIndexOf(".");
        if(start!=-1){
            return filename.substring(start+1,filename.length());
        }else{
            return null;
        }
    }

    /**
     * 拼接公共参数
     * @param url
     * @param commonField
     * @return
     */
    public static String getMosaicParameter(String url, List<NameValuePair> commonField){
        if (TextUtils.isEmpty(url))
            return "";
        if (url.contains("?")) {
            url = url + "&";
        } else {
            url = url + "?";
        }
        url += getCommonFieldString(commonField);
        return url;
    }

    /**
     *
     * @param commonField
     * @return
     */
    private static String getCommonFieldString(List<NameValuePair> commonField){
        StringBuffer sb = new StringBuffer();
        try{
            int i=0;
            for (NameValuePair item:commonField) {
                if(i>0){
                    sb.append("&");
                }
                sb.append(item.getName());
                sb.append('=');
                sb.append(URLEncoder.encode(item.getValue(),"utf-8"));
                i++;
            }
        }catch (Exception e){

        }
        return  sb.toString();
    }
}
