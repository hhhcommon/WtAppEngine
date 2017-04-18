package com.woting.cm.core.oss.persis.po;

public class OssConfigPo {
	
	private String Endpoint;
	private String AccessKeyId;
	private String AccessKeySecret;
	private String BucketName;
	private String TempFile;
	
	public String getEndpoint() {
		return Endpoint;
	}
	public void setEndpoint(String endpoint) {
		Endpoint = endpoint;
	}
	public String getAccessKeyId() {
		return AccessKeyId;
	}
	public void setAccessKeyId(String accessKeyId) {
		AccessKeyId = accessKeyId;
	}
	public String getAccessKeySecret() {
		return AccessKeySecret;
	}
	public void setAccessKeySecret(String accessKeySecret) {
		AccessKeySecret = accessKeySecret;
	}
	public String getBucketName() {
		return BucketName;
	}
	public void setBucketName(String bucketName) {
		BucketName = bucketName;
	}
	public String getTempFile() {
		if (TempFile!=null && TempFile.length()>0) {
			if (TempFile.lastIndexOf("/")!=(TempFile.length()-1)) {
				TempFile += "/";
			}
		}
		return TempFile;
	}
	public void setTempFile(String tempFile) {
		TempFile = tempFile;
	}
}
