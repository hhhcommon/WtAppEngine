package com.woting.appengine.content.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class FileUtils {

	public static boolean writeFile(String jsonstr, File file) {
		try {
			OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			BufferedWriter writer = new BufferedWriter(write);
			writer.write(jsonstr);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (file.exists())
			return true;
		else
			return false;
	}
	
	public static String readFile(File file) {
		String sb = "";
		if (!file.exists()) {
			return null;
		}
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");
			BufferedReader reader = new BufferedReader(read);
			String line;
			while ((line = reader.readLine()) != null) {
				sb += line;
			}
			read.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb;
	}
	
	public static void writeContentInfo(String key, String jsonstr) {
		try {
			download("http://www.wotingfm.com/dataCenter/contentinfo", key+".json", "/opt/dataCenter/contentinfo/"+key+".json");
		} catch (Exception e) {
			e.printStackTrace();
		}
		File file = FileUtils.createFile("/opt/dataCenter/contentinfo/"+key+".json");
		writeFile(jsonstr, file);
	}
	
	public static String readContentInfo(String key) {
		try {
			download("http://www.wotingfm.com/dataCenter/contentinfo/"+key+".json", key+".json", "/opt/dataCenter/contentinfo/");
		} catch (Exception e) {
			e.printStackTrace();
		}
		File file = FileUtils.createFile("/opt/dataCenter/contentinfo/"+key+".json");
		return readFile(file);
	}

	public static File createFile(String path) {
		File file = new File(path);
		try {
			if (!file.exists()) {
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				} else {
					file.createNewFile();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}


	public static void download(String urlString, String filename, String savePath) throws Exception {
		// 构造URL
		URL url = new URL(urlString);
		// 打开连接
		URLConnection con = url.openConnection();
		// 设置请求超时为5s
		con.setConnectTimeout(50 * 1000);
		// 输入流
		InputStream is = con.getInputStream();
		// 1K的数据缓冲
		byte[] bs = new byte[1024];
		// 读取到的数据长度
		int len;
		// 输出的文件流
		File sf = new File(savePath);
		if (!sf.exists()) {
			sf.mkdirs();
		}
		OutputStream os = new FileOutputStream(sf.getPath() + "/" + filename);
		// 开始读取
		while ((len = is.read(bs)) != -1) {
			os.write(bs, 0, len);
		}
		// 完毕，关闭所有链接
		os.close();
		is.close();
	}
}
