package com.imooc.bilibili.service.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * MD5加密
 * 单向加密算法
 * 特点：加密速度快，不需要秘钥，但是安全性不高，需要搭配随机盐值使用
 */
public class MD5Util {

    public static String sign(String content, String salt, String charset) {
        content = content + salt;
        return DigestUtils.md5Hex(getContentBytes(content, charset));
    }

    public static boolean verify(String content, String sign, String salt, String charset) {
        content = content + salt;
        String mysign = DigestUtils.md5Hex(getContentBytes(content, charset));
        return mysign.equals(sign);
    }

    private static byte[] getContentBytes(String content, String charset) {
        if (!"".equals(charset)) {
            try {
                return content.getBytes(charset);
            } catch (UnsupportedEncodingException var3) {
                throw new RuntimeException("MD5签名过程中出现错误,指定的编码集错误");
            }
        } else {
            return content.getBytes();
        }
    }

    /**
     * 获取文件md5加密后的字符串
     */
    public static String getFileMD5(MultipartFile file) throws Exception {
        //.getInputStream 输入流，都是对底层流的加工处理
        InputStream fis = file.getInputStream();
        //输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int byteRead;
        // fis-->baos，将输入流的内容写到输出流中
        // fis.read(buffer)：从输入流中读取一些字节并将它们存储到缓冲区数组，实际读取的字节数以整数形式返回
        // baos.write：从偏移量 off 开始的指定字节数组中写入 len 个字节到此 ByteArrayOutputStream。
        while ((byteRead = fis.read(buffer)) > 0) {
            baos.write(buffer, 0, byteRead);
        }
        fis.close();
        return DigestUtils.md5Hex(baos.toByteArray());
    }

}