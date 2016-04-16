package com.woting.plugins.sms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

import com.spiritdata.framework.util.DateUtils;

@SuppressWarnings("restriction")
public abstract class SendSMS {
    protected static String sendUrl="http://api.shumi365.com:8090/sms/send.do";
    protected static String userId="400179";
    protected static String password="453881";

    /**
     * 发送短信
     * @param phoneNum 对方电话号码
     * @param checkNum 验证码
     * @param oper 操作描述
     * @return 返回的串
     */
    public static String sendSms(final String phoneNum, final String checkNum, final String oper) {
        BASE64Encoder encoder = new BASE64Encoder();

        HttpClientBuilder clientBuilder=HttpClientBuilder.create();
        CloseableHttpClient httpClient=clientBuilder.build();
        HttpPost httppost=new HttpPost(sendUrl);
        try {
            String _date=DateUtils.convert2LocalStr("yyyyMMddHHmmss", new Date());
            String _msg="【我听科技】验证码："+checkNum+"，您正使用“"+oper+"”功能。此验证码60秒后过期，请注意验证码的保密！";

            List<NameValuePair> params=new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("userid", userId));
            params.add(new BasicNameValuePair("pwd", getMD5(password+_date)));
            params.add(new BasicNameValuePair("timespan", _date));
            params.add(new BasicNameValuePair("mobile", phoneNum));
            params.add(new BasicNameValuePair("content", encoder.encode(_msg.getBytes("GBK"))));

            httppost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            HttpResponse res=httpClient.execute(httppost);
            int status=res.getStatusLine().getStatusCode();
            if (status==HttpStatus.SC_OK) {
                HttpEntity entity=res.getEntity();
                if (entity!=null) return EntityUtils.toString(entity, "GBK");
            } else {
                int count=0;
                while (count++<3) {
                    Thread.sleep(100);
                    res=httpClient.execute(httppost);
                    status=res.getStatusLine().getStatusCode();
                    if (status==HttpStatus.SC_OK) {
                        HttpEntity entity=res.getEntity();
                        if (entity!=null) return EntityUtils.toString(entity, "GBK");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httppost.abort();
        }
        return "";
    }

    /*
     * 方法名称：getMD5
     * 功    能：字符串MD5加密
     * 参    数：待转换字符串
     * 返 回 值：加密之后字符串
     */
    private static String getMD5(String sourceStr){
        String resultStr = "";
        try {
            byte[] temp = sourceStr.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(temp);
            // resultStr = new String(md5.digest());
            byte[] b = md5.digest();
            for (int i = 0; i < b.length; i++) {
                char[] digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
                char[] ob = new char[2];
                ob[0] = digit[(b[i] >>> 4) & 0X0F];
                ob[1] = digit[b[i] & 0X0F];
                resultStr += new String(ob);
            }
            return resultStr;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        sendSms("13910672205", "223456", "测试阶段");
    }
}