package com.qiniu.common;

import java.io.IOException;

public class QiniuException extends IOException {
	public final Response response;
    private String error;


    public QiniuException(Response response) {
        super(response != null ? response.getInfo() : null);
        this.response = response;
    }

    public QiniuException(Exception e) {
        this(e, null);
    }

    public QiniuException(Exception e, String msg) {
        super(msg, e);
        this.response = null;
        this.error = msg;
    }

    public String url() {
        return response.url();
    }

    public int code() {
        return response == null ? -1 : response.statusCode;
    }

    public String error() {
        if (error != null) {
            return error;
        }
        if (response == null || response.statusCode / 100 == 2 || !response.isJson()) {
            return null;
        }
        Error e = null;
        try {
            e = response.jsonToObject(Error.class);
        } catch (QiniuException e1) {
            e1.printStackTrace();
        }
        error = e == null ? "" : e.toString();
        return error;
    }
}
