package com.woting.cm.core.oss.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.cm.core.oss.persis.po.OssConfigPo;

public class OssUtils {
	
	/**
	 * OSS上传文件
	 * @param key 文件Object
	 * @param content 字符串
	 * @param isOrNoDelete 是否删除原文件
	 * @return
	 */
	public static boolean upLoadObject(String key, String content, boolean isOrNoDelete) {
		OSSClient ossClient = null;
		try {
			if (content!=null && key!=null) {
				OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
				if (ossConfigPo!=null) {
					ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
					if (ossClient!=null) {
						File file = createFile(ossConfigPo.getTempFile()+SequenceUUID.getPureUUID()+".json");
						writeFile(content, file);
						ObjectMetadata meta = new ObjectMetadata();
						meta.setContentLength(content.length());
						meta.setCacheControl("no-cache");
						meta.setHeader("Pragma", "no-cache");
						meta.setContentType(contentType(file.getName().substring(file.getName().lastIndexOf("."))));
						meta.setContentDisposition("inline;filename=" + file.getName());
						ossClient.putObject(ossConfigPo.getBucketName(), key, file, meta);
						if (ossClient.doesObjectExist(ossConfigPo.getBucketName(), key)) {
						    if (isOrNoDelete) deleteFile(file);
						    return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return false;
	}
	
	/**
	 * OSS上传文件
	 * @param key 文件Object
	 * @param file 文件
	 * @param isOrNoDelete 是否删除原文件
	 * @return
	 */
	public static boolean upLoadObject(String key, File file, boolean isOrNoDelete) {
		OSSClient ossClient = null;
		try {
			if (file!=null && key!=null) {
				OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
				if (ossConfigPo!=null) {
					ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
					if (ossClient!=null) {
						ObjectMetadata meta = new ObjectMetadata();
						meta.setContentLength(file.getTotalSpace());
						meta.setCacheControl("no-cache");
						meta.setHeader("Pragma", "no-cache");
						meta.setContentType(contentType(file.getName().substring(file.getName().lastIndexOf("."))));  
						meta.setContentDisposition("inline;filename=" + file.getName()); 
						ossClient.putObject(ossConfigPo.getBucketName(), key, file, meta);
						if (ossClient.doesObjectExist(ossConfigPo.getBucketName(), key)) {
							if (isOrNoDelete) deleteFile(file);
							return true;	
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return false;
	}
	
	/**
	 * 上传网络流
	 * @param key 文件Object
	 * @param url 网络流地址
	 * @return
	 */
	public static boolean upLoadObject(String key, InputStream in) {
		OSSClient ossClient = null;
		try {
			if (in!=null && key!=null) {
				OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
				if (ossConfigPo!=null) {
					ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
					if (ossClient!=null) {
						ObjectMetadata meta = new ObjectMetadata();
						meta.setContentLength(in.available());
						meta.setCacheControl("no-cache");
						meta.setHeader("Pragma", "no-cache");
						meta.setContentType("text/html");
//						meta.setContentDisposition("inline;filename=");
						ossClient.putObject(ossConfigPo.getBucketName(), key, in, meta);
						if (ossClient.doesObjectExist(ossConfigPo.getBucketName(), key)) {
							in.close();
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return false;
	}
	
	/**
	 * 上传网络流
	 * @param key 文件Object
	 * @param url 网络流地址
	 * @return
	 */
	public static boolean upLoadObject(String key, String url) {
		OSSClient ossClient = null;
		try {
			if (url!=null && url.length()>0 && key!=null) {
				OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
				if (ossConfigPo!=null) {
					ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
					if (ossClient!=null) {
						InputStream in = new URL(url).openStream();
						if (in != null) {
							ObjectMetadata meta = new ObjectMetadata();
							meta.setCacheControl("no-cache");
							meta.setHeader("Pragma", "no-cache");
							meta.setContentType("text/html");
							ossClient.putObject(ossConfigPo.getBucketName(), key, in, meta);
							if (ossClient.doesObjectExist(ossConfigPo.getBucketName(), key)) {
								in.close();
								return true;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return false;
	}
	
	public static InputStream getObjectToInputStream(String key) {
		OSSClient ossClient = null;
		try {
			OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
			if (ossConfigPo!=null) {
				ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
				if (ossClient!=null) {
					OSSObject ossObject = ossClient.getObject(ossConfigPo.getBucketName(), key);
					if (ossObject!=null) {
						InputStream in = ossObject.getObjectContent();
						if (in!=null) {
							return in;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return null;
	}
	
	public static File getObjectToFile(String key) {
		InputStream is = getObjectToInputStream(key);
		OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
		if (is!=null && ossConfigPo!=null) {
			try {
				String name = key.substring(key.lastIndexOf("/"), key.length());
				File file = createFile(ossConfigPo.getTempFile()+name);
				if (writeFile(is, file)) {
					return file;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	public static String getObjectToString(String key) {
		InputStream is = getObjectToInputStream(key);
		if (is!=null) {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(is));
				StringBuffer buffer = new StringBuffer();
				String line = "";
				while ((line = in.readLine()) != null){
					buffer.append(line);
				}
				is.close();
				return buffer.toString();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	/**
	 * 图片缩放
	 * @param key 图片Object
	 * @param ResizePer 缩放大小
	 * @return
	 * 默认进行格式转换，转为png格式
	 */
	public static InputStream makePictureResize(String key, int ResizePer) {
		OSSClient ossClient = null;
		try {
			OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
			if (ossConfigPo!=null) {
				ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
				if (ossClient!=null) {
					String style = "image/resize,m_lfit,w_"+ResizePer+",h_"+ResizePer+",limit_0/auto-orient,0";  
					GetObjectRequest request = new GetObjectRequest(ossConfigPo.getBucketName(), key);
					request.setProcess(style);
					OSSObject ossObject = ossClient.getObject(request);
					if (ossObject!=null) {
					    InputStream in = ossObject.getObjectContent();
						if (in!=null) {
							return in;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return null;
	}
	
	/**
	 * 
	 * @param key 图片Object
	 * @param WidhtResizePer 宽缩放大小
	 * @param HighResizePer 高缩放大小
	 * @return
	 */
	public static InputStream makePictureResize(String key, int WidhtResizePer, int HighResizePer) {
		OSSClient ossClient = null;
		try {
			OssConfigPo ossConfigPo = (OssConfigPo) getBean("ossconfig");
			if (ossConfigPo!=null) {
				ossClient = new OSSClient(ossConfigPo.getEndpoint(), ossConfigPo.getAccessKeyId(), ossConfigPo.getAccessKeySecret());
				if (ossClient!=null) {
					String style = "image/resize,m_lfit,w_"+WidhtResizePer+",h_"+HighResizePer+",limit_0/auto-orient,0/quality,q_100";  
					GetObjectRequest request = new GetObjectRequest(ossConfigPo.getBucketName(), key);
					request.setProcess(style);
					OSSObject ossObject = ossClient.getObject(request);
					if (ossObject!=null) {
					    InputStream in = ossObject.getObjectContent();
						if (in!=null) {
							return in;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ossClient!=null) ossClient.shutdown();
		}
		return null;
	}
	
	private static boolean writeFile(String jsonstr, File file) {
		try {
			OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			BufferedWriter writer = new BufferedWriter(write);
			writer.write(jsonstr);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (file.exists()) return true;
		else return false;
	}
	
	private static boolean writeFile(InputStream is, File file) {
		try {
			OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			BufferedWriter writer = new BufferedWriter(write);
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			StringBuffer buffer = new StringBuffer();
			String line = "";
			while ((line = in.readLine()) != null){
				buffer.append(line);
			}
			writer.write(buffer.toString());
			writer.close();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (file.exists()) return true;
		else return false;
	}
	
	private static File createFile(String path) {
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
	
	private static String readFile(File file) {
		String sb = "";
		if (!file.exists()) return null;
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
	
	private static void deleteFile(File file) {
		if (file!=null && file.exists()) {
			file.delete();
		}
	}
	
	private static Object getBean(String beanName) {
		ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            return WebApplicationContextUtils.getWebApplicationContext(sc).getBean(beanName);
        }
		return null;
	}
	
	/**
     * 判断OSS服务文件上传时文件的contentType
     * @param FilenameExtension 文件后缀 
     * @return String
     */
     public static String contentType(String FilenameExtension){
    	FilenameExtension = FilenameExtension.toLowerCase();
        if(FilenameExtension.equals("BMP")||FilenameExtension.equals("bmp")){return "image/bmp";}  
        if(FilenameExtension.equals("GIF")||FilenameExtension.equals("gif")){return "image/gif";}  
        if(FilenameExtension.equals("JPEG")||FilenameExtension.equals("jpeg")||FilenameExtension.equals("JPG")||FilenameExtension.equals("jpg")||
           FilenameExtension.equals("PNG")||FilenameExtension.equals("png")){return "image/jpeg";}  
        if(FilenameExtension.equals("HTML")||FilenameExtension.equals("html")){return "text/html";}
        if(FilenameExtension.equals("TXT")||FilenameExtension.equals("txt")){return "text/plain";}  
        if(FilenameExtension.equals("VSD")||FilenameExtension.equals("vsd")){return "application/vnd.visio";}  
        if(FilenameExtension.equals("PPTX")||FilenameExtension.equals("pptx")||
            FilenameExtension.equals("PPT")||FilenameExtension.equals("ppt")){return "application/vnd.ms-powerpoint";}  
        if(FilenameExtension.equals("DOCX")||FilenameExtension.equals("docx")||  
            FilenameExtension.equals("DOC")||FilenameExtension.equals("doc")){return "application/msword";}  
        if(FilenameExtension.equals("XML")||FilenameExtension.equals("xml")){return "text/xml";}  
        return "text/html";
     }
}
