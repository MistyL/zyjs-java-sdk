package com.qiniu;

import java.util.ArrayList;

import com.qiniu.common.AsyncCallback;
import com.qiniu.common.Client;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Response;
import com.qiniu.common.ResumeBlockInfo;
import com.qiniu.util.Crc32;
import com.qiniu.util.StringMap;
import com.qiniu.util.StringUtils;
import com.qiniu.util.UrlSafeBase64;

public class UploadManager {
    private ArrayList<String> contexts;
    private Configuration configuration;
    private Client client;
    private String host;
    private int retryMax;
    private long size;

    /**
     * 构建分片上传文件的对象
     * @throws QiniuException 
     */
    public UploadManager(Configuration configuration,String upToken) throws QiniuException {
    		this.client = new Client(configuration);
    		this.configuration = configuration;
    		this.contexts = new ArrayList<>();
    		this.size = 0;
    		this.host = configuration.upHost(upToken);
    }
    
    /**
     * 上传块数据 
     * 除文件的最后一块大小可以不超过 4M ，其他块大小必须为 4 * 1024 * 1024 Byte
     * @param upToken 上传token
     * @param block 数据块内容
     * @param index 数据块索引
     * @param blocksize 数据块大小
     * @return
     * @throws QiniuException
     */
    public Response upload(String upToken,byte[] block,int index,int blocksize) throws QiniuException {
        if (this.host == null) {
        		host = configuration.upHost(upToken);
		}
        long crc = Crc32.bytes(block, 0, blocksize);
        Response response = null;
        try {
            response = makeBlock(block, blocksize,upToken);
        } catch (QiniuException e) {
            response = Response.createError(null, "", -1,e.toString());
            return response;
        }

        ResumeBlockInfo blockInfo = response.jsonToObject(ResumeBlockInfo.class);
        if (blockInfo.crc32 != crc) {
        		//crc32 校验
            response = Response.createError(null, "", -1, "block's crc32 is not match");
            return response;
        }
        contexts.add(index, blockInfo.ctx);
        size += blocksize;
        
        return response;
    }
    
    /**
     * 异步上传块数据
     * @param upToken 上传token
     * @param block 块数据
     * @param index 块索引
     * @param blocksize 块大小，除最后一块外，其他块大小必须是 4M
     * @param callback 回掉函数
     * @throws QiniuException
     */
    public void uploadAsync(String upToken,byte[] block,int index,int blocksize,AsyncCallback callback) throws QiniuException {
	    	if (this.host == null) {
	    		host = configuration.upHost(upToken);
		}
	    long crc = Crc32.bytes(block, 0, blocksize);
	    	String url = host + "/mkblk/" + blocksize;
	    	asyncPost(url, block, upToken, new AsyncCallback() {
			@Override
			public void complete(Response r) {
				// TODO Auto-generated method stub
				ResumeBlockInfo blockInfo;
				try {
					blockInfo = r.jsonToObject(ResumeBlockInfo.class);
					//crc32 校验
					if (blockInfo.crc32 != crc) {
					     Response response = Response.createError(null, "", -1, "block's crc32 is not match");
					     callback.complete(response);
					}
					contexts.add(index, blockInfo.ctx);
					size += blocksize;
				} catch (QiniuException e) {
					// TODO Auto-generated catch block
					Response response = Response.createError(null, "", -1, e.error());
					callback.complete(response);
				}
			}
		});
    }
    
    /**
     * 合并文件
     * 可以指定 文件key，文件 mimeType，以及自定义变量
     * 自定变量可参考：https://developer.qiniu.com/kodo/api/1287/mkfile
     * @param upToken 上传token
     * @param key 上传后保存文件名
     * @param mime 文件 mimeType
     * @param params 自定义变量
     * @return
     * @throws QiniuException
     */
    public Response makeFile(String upToken,String key, String mime,StringMap params) throws QiniuException {
        String url = fileUrl(key, mime,params);
        String s = StringUtils.join(contexts, ",");
        return post(url, StringUtils.utf8Bytes(s),upToken);
    }
    
    private Response makeBlock(byte[] block, int blockSize,String upToken) throws QiniuException {
        String url = host + "/mkblk/" + blockSize;
        return post(url, block, 0, blockSize,upToken);
    }

    private String fileUrl(String key,String mime,StringMap params) {
        String url = host + "/mkfile/" + size;
        final StringBuilder b = new StringBuilder(url);
        if (mime != null) {
			b.append("/mimeType/");
			b.append(UrlSafeBase64.encodeToString(mime));
		}
        if (key != null) {
            b.append("/key/");
            b.append(UrlSafeBase64.encodeToString(key));
        }
        if (params != null) {
            params.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    b.append("/");
                    b.append(key);
                    b.append("/");
                    b.append(UrlSafeBase64.encodeToString("" + value));
                }
            });
        }
        return b.toString();
    }
    
    private void asyncPost(String url, byte[] data, String upToken,AsyncCallback callback) {
    		client.asyncPost(url, data, 0, data.length, new StringMap().put("Authorization", "UpToken " + upToken), Client.DefaultMime, callback);
    }

    private Response post(String url, byte[] data,String upToken) throws QiniuException {
        return client.post(url, data, new StringMap().put("Authorization", "UpToken " + upToken));
    }

    private Response post(String url, byte[] data, int offset, int size,String upToken) throws QiniuException {
        return client.post(url, data, offset, size, new StringMap().put("Authorization", "UpToken " + upToken),
                Client.DefaultMime);
    }
}
