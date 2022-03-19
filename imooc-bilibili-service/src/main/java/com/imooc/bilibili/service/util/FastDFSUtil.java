package com.imooc.bilibili.service.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.imooc.bilibili.domain.exception.ConditionException;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/18
 **/
@Component
public class FastDFSUtil {
    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PATH_KEY = "path-key:";

    private static final String UPLOADED_SIZE_KEY = "uploaded-size-key:";

    private static final String UPLOADED_NO_KEY = "uploaded-no-key:";

    private static final String DEFAULT_GROUP = "group1";

    private static final int SLICE_SIZE = 1024 * 1024 * 2;


    @Value("${fdfs.http.storage-addr}")
    private String httpFdfsStorageAddr;
    @Value("${mooc.bilibili.slices.address}")
    private String tempSliceFilesAddr;


    /**
     * 获取文件类型
     */
    public String getFileType(MultipartFile file){
        if(file == null){
            throw new ConditionException("非法文件！");
        }
        String fileName = file.getOriginalFilename();
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index+1);
    }

    //上传
    public String uploadCommonFile(MultipartFile file) throws Exception {
        Set<MetaData> metaDataSet = new HashSet<>();
        String fileType = this.getFileType(file);
        StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(), file.getSize(), fileType, metaDataSet);
        return storePath.getPath();
    }

    public String uploadCommonFile(File file, String fileType) throws Exception {
        Set<MetaData> metaDataSet = new HashSet<>();
        StorePath storePath = fastFileStorageClient.uploadFile(new FileInputStream(file),
                file.length(), fileType, metaDataSet);
        return storePath.getPath();
    }

    //上传可以断点续传的文件
    public String uploadAppenderFile(MultipartFile file) throws Exception{
        String fileType = this.getFileType(file);
        StorePath storePath = appendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, file.getInputStream(), file.getSize(), fileType);
        return storePath.getPath();
    }

    public void modifyAppenderFile(MultipartFile file, String filePath, long offset) throws Exception{
        appendFileStorageClient.modifyFile(DEFAULT_GROUP, filePath, file.getInputStream(), file.getSize(), offset);
    }

    /**
     * 分片上传文件，返回上传后的地址
     */
    public String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        if (file == null || sliceNo == null || totalSliceNo == null) {
            throw new ConditionException("参数异常！");
        }
        //第一个分片上传后的存储路径
        String pathKey = PATH_KEY + fileMd5;
        //已上传分片文件的大小
        String uploadedSizeKey = UPLOADED_SIZE_KEY + fileMd5;
        //已上传的分片数
        String uploadedNoKey = UPLOADED_NO_KEY + fileMd5;
        //已上传的文件大小：0-上传的为第1个分片
        String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey);
        // 感觉这里是每个变量分别进行判断
        Long uploadedSize = 0L;
        if (!StringUtil.isNullOrEmpty(uploadedSizeStr)) {
            uploadedSize = Long.valueOf(uploadedSizeKey);
        }
        if (sliceNo == 1) { // 上传的是第1个分片
            String path = this.uploadAppenderFile(file);
            if (StringUtil.isNullOrEmpty(path)) {
                throw new ConditionException("上传失败！");
            }
            redisTemplate.opsForValue().set(pathKey, path);
            redisTemplate.opsForValue().set(uploadedNoKey, "1");
        } else {
            String filePath = redisTemplate.opsForValue().get(pathKey);
            if (StringUtil.isNullOrEmpty(filePath)) {
                throw new ConditionException("上传失败！");
            }
            this.modifyAppenderFile(file, filePath, uploadedSize);
            redisTemplate.opsForValue().increment(uploadedNoKey);
        }
        //此处为文件上传前后分界
        //修改uploadedSize：已上传分片文件的大小
        uploadedSize += file.getSize();
        redisTemplate.opsForValue().set(uploadedSizeKey, String.valueOf(uploadedSize));
        // 如果所有分片上传完毕，则清空redis中相关的key-value
        String uploadedNostr = redisTemplate.opsForValue().get(uploadedNoKey);
        Integer uploadedNo = Integer.valueOf(uploadedNostr);
        String resultPath = ""; //文件上传后存储的地址
        if (uploadedNo.equals(totalSliceNo)) {
            resultPath = redisTemplate.opsForValue().get(pathKey);
            List<String> keyList = Arrays.asList(uploadedNoKey, pathKey, uploadedSizeKey);
            redisTemplate.delete(keyList);
        }
        return resultPath;
    }

    /**
     * 文件分片
     */
    public void convertFileToSlices(MultipartFile multipartFile) throws Exception {
        String fileType = this.getFileType(multipartFile);
        //生成临时文件，将MultipartFile转为File
        File tempFile = this.multipartFileToFile(multipartFile);
        //.length 返回此抽象路径名表示的文件的长度（以字节为单位）
        long fileLength = tempFile.length();
        int count = 1;
        //实际分片
        for (int i = 0; i < fileLength; i += SLICE_SIZE) {
            // RandomAccessFile：此类的实例支持读取和写入随机访问文件。
            RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "r");
            randomAccessFile.seek(i);
            byte[] bytes = new byte[SLICE_SIZE];
            //从此文件中读取最多 b.length 个字节的数据到一个字节数组中，返回读入缓冲区的总字节数
            int len = randomAccessFile.read(bytes);
            //分片文件全路径名称
            String path = tempSliceFilesAddr + count + "." + fileType;
            //String path = "D:\\mooc-bilibili\\tempFile\\" + count + "." + fileType;
            File slice = new File(path);
            FileOutputStream fos = new FileOutputStream(slice);
            fos.write(bytes, 0, len);
            fos.close();
            randomAccessFile.close();
            count++;
        }
        //删除临时文件
        tempFile.delete();
    }

    /**
     * 将(Spirng)MultipartFile类型的文件转为(Java原生)File类型的文件
     * 注意：此File文件为临时文件，后续需要手动删除
     */
    public File multipartFileToFile(MultipartFile multipartFile) throws Exception {
        String originalFileName = multipartFile.getOriginalFilename();
        String[] fileName = originalFileName.split("\\.");
        File file = File.createTempFile(fileName[0], "." + fileName[1]);
        multipartFile.transferTo(file);
        return file;
    }

    //删除
    public void deleteFile(String filePath) {
        fastFileStorageClient.deleteFile(filePath);
    }


    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String path) throws Exception {
        FileInfo fileInfo = fastFileStorageClient.queryFileInfo(DEFAULT_GROUP, path);
        long totalFileSize = fileInfo.getFileSize();
        String url = httpFdfsStorageAddr + path;
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, Object> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }
        String rangeStr = request.getHeader("Range");
        String[] range;
        if (StringUtil.isNullOrEmpty(rangeStr)) {
            rangeStr = "bytes=0-" + (totalFileSize - 1);
        }
        range = rangeStr.split("bytes=|-");
        long begin = 0;
        if (range.length >= 2) {
            begin = Long.parseLong(range[1]);
        }
        long end = totalFileSize - 1;
        if (range.length >= 3) {
            end = Long.parseLong(range[2]);
        }
        long len = (end - begin) + 1;
        String contentRange = "bytes " + begin + "-" + end + "/" + totalFileSize;
        response.setHeader("Content-Range", contentRange);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "video/mp4");
        response.setContentLength((int) len);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        HttpUtil.get(url, headers, response);
    }

    public void downLoadFile(String url, String localPath) {
        fastFileStorageClient.downloadFile(DEFAULT_GROUP, url,
                new DownloadCallback<String>() {
                    @Override
                    public String recv(InputStream ins) throws IOException {
                        File file = new File(localPath);
                        OutputStream os = new FileOutputStream(file);
                        int len = 0;
                        byte[] buffer = new byte[1024];
                        while ((len = ins.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        os.close();
                        ins.close();
                        return "success";
                    }
                });
    }
}
