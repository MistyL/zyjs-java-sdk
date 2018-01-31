package com.qiniu;

import java.util.Random;
import org.junit.Test;

import com.qiniu.common.AsyncCallback;
import com.qiniu.common.Auth;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Response;
import com.qiniu.common.Zone;

public class UploadManagerTest {
	static final Random r = new Random();
	static final Auth auth = Auth.create("", "");
	static int len = 0;
	
    @Test
    public void testUploadManager() throws Throwable {
    		String token = auth.uploadToken("cdntest", "key_2");
    		Configuration configuration = new Configuration(Zone.zone0());
    		UploadManager uploadManager = new UploadManager(configuration,token);
    		for (int i = 0; i < 4; i++) {
    			byte[] block = getByte(4 * 1024 * 1024);
        		uploadManager.upload(token,block, i, block.length);
		}
    		//注意，如果 uploadToken 中指定了key，这里的 key 要和 uploadToken 中的 key 相同
    		uploadManager.makeFile(token,"key_2",null,null);
    }
    
    @Test
    public void testUploadManagerAsync() throws QiniuException {
    		String token = auth.uploadToken("cdntest","key_3");
    		Configuration configuration = new Configuration(Zone.zone0());
    		UploadManager uploadManager = new UploadManager(configuration, token);
    		
    		for (int i = 0; i < 4; i++) {
    			byte[] block = getByte(4 * 1024 * 1024);
    			uploadManager.uploadAsync(token, block, i, block.length, new AsyncCallback() {
					
				@Override
				public void complete(Response r) {
					// TODO Auto-generated method stub
					len++;
					if (len == 4) {
						try {
							//mkfile 前要确保所有块均已上传成功
							uploadManager.makeFile(token, "key_3", null, null);
						} catch (QiniuException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}
    }
    
    private byte[] getByte(int len) {
        byte b = (byte) r.nextInt();
        byte[] bs = new byte[len];

        for (int i = 1; i < len; i++) {
            bs[i] = b;
        }

        bs[10] = (byte)r.nextInt();
        bs[9] = (byte) r.nextInt();
        bs[8] = (byte) r.nextInt();
        bs[7] = (byte) r.nextInt();
        bs[6] = (byte) r.nextInt();
        bs[5] = (byte) r.nextInt();
        bs[4] = (byte) r.nextInt();
        bs[3] = (byte) r.nextInt();
        bs[3] = (byte) r.nextInt();
        bs[1] = (byte) r.nextInt();
        bs[0] = (byte) r.nextInt();

        bs[len - 2] = '\r';
        bs[len - 1] = '\n';
        return bs;
    }
}
