package com.jsls.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * http 工具类
 * 
 * @author YM10177
 *
 */
public class HttpUtils {
    public static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private static class HttpClientHolder {
        private static final CloseableHttpClient HTTP_CLIENT;
        static {
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            // 连接池中最大连接数
            httpClientBuilder.setMaxConnTotal(200);
            /**
             * 分配给同一个route(路由)最大的并发连接数。
             * route：运行环境机器 到 目标机器的一条线路。
             * 举例来说，我们使用HttpClient的实现来分别请求 www.baidu.com 的资源和 www.bing.com
             * 的资源那么他就会产生两个route。
             */
            httpClientBuilder.setMaxConnPerRoute(100)
                    .evictIdleConnections(60, TimeUnit.SECONDS) // 定期回收空闲连接
                    .evictExpiredConnections() // 定期回收过期连接
                    .setConnectionTimeToLive(60, TimeUnit.SECONDS)// 连接存活时间，如果不设置，则根据长连接信息决定
                    .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
                    .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(3, false));// false 不重试非幂等请求

            RequestConfig requestConfig = RequestConfig.custom()
                    // 从连接池中获取连接的超时时间
                    .setConnectionRequestTimeout(2000)
                    // 与服务器连接超时时间：httpclient会创建一个异步线程用以创建socket连接，此处设置该socket的连接超时时间
                    .setConnectTimeout(5000)
                    // socket读数据超时时间：从服务器获取响应数据的超时时间
                    .setSocketTimeout(5000)
                    .build();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            HTTP_CLIENT = httpClientBuilder.build();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        HTTP_CLIENT.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 将java对象转为from表单的传参形式
     * 
     * @param val
     * @return
     */
    private static String useParamValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Collection) {
            Collection<?> cval = (Collection<?>) val;
            int i = 0;
            StringBuilder sb = new StringBuilder();
            for (Object vi : cval) {
                Object uvi = useParamValue(vi);
                if (i++ > 0) {
                    sb.append(",").append(uvi);
                } else {
                    sb.append(uvi);
                }
            }
            return sb.toString();
        } else if (val instanceof Object[]) {
            Object[] aval = (Object[]) val;
            int i = 0;
            StringBuilder sb = new StringBuilder();
            for (Object vi : aval) {
                Object uvi = useParamValue(vi);
                if (i++ > 0) {
                    sb.append(",").append(uvi);
                } else {
                    sb.append(uvi);
                }
            }
            return sb.toString();
        } else if (val instanceof Date) {
            return DateUtils.formatDate((Date) val, DateUtils.DATE_FORMAT_DATETIME);
        } else if (val instanceof LocalDate) {
            Date ld = DateUtils.asDate((LocalDate) val);
            return DateUtils.formatDate((Date) ld, DateUtils.DATE_FORMAT_DATE);
        } else if (val instanceof LocalDateTime) {
            Date ldt = DateUtils.asDate((LocalDateTime) val);
            return DateUtils.formatDate((Date) ldt, DateUtils.DATE_FORMAT_DATETIME);
        }
        return val.toString();
    }

    /**
     * data转为form表单
     * 
     * @param data
     * @return
     */
    public static List<NameValuePair> dataToNameValuePairs(Map<String, ?> data) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (CollectionUtils.isEmpty(data)) {
            return params;
        }
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            NameValuePair pair = new BasicNameValuePair(entry.getKey(), useParamValue(value));
            params.add(pair);
        }
        return params;
    }

    public static class MultipartInfo {
        private String fileKey;
        private String fileName;
        private InputStream inputStream;

        public String getFileKey() {
            return fileKey;
        }

        public void setFileKey(String fileKey) {
            this.fileKey = fileKey;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public String toString() {
            return "MultipartInfo [fileKey=" + fileKey + ", fileName=" + fileName + ", inputStream=" + inputStream
                    + "]";
        }
    }

    public static String upload(String url, List<MultipartInfo> multipartInfos, Map<String, ?> paramMap) {
        HttpPost httpPost = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (MultipartInfo multipartInfo : multipartInfos) {
            String fileName;
            try {
                fileName = java.net.URLEncoder.encode(multipartInfo.getFileName(), "UTF-8");
                builder.addBinaryBody(multipartInfo.getFileKey(), multipartInfo.getInputStream(),
                        ContentType.MULTIPART_FORM_DATA, fileName);
            } catch (UnsupportedEncodingException e) {
                // never
            }

        }
        ContentType contentType = ContentType.create("text/plain", Consts.UTF_8);
        for (Map.Entry<String, ?> entry : paramMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            builder.addTextBody(entry.getKey(), useParamValue(entry.getValue()), contentType);
        }
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);
        return consumeRequest(httpPost);
    }

    public static String upload(String url, MultipartInfo multipartInfo, Map<String, Object> paramMap) {
        return upload(url, Collections.singletonList(multipartInfo), paramMap);
    }

    /**
     * get 请求
     * 
     * @param url
     * @param data
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     * @throws UnsupportedOperationException
     */
    public static String get(String url) {
        return consumeRequest(new HttpGet(url));
    }

    /**
     * get 请求
     * 
     * @param url
     * @param data
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     * @throws UnsupportedOperationException
     */
    public static String get(String url, Map<String, ?> data) {
        return consumeRequest(buildUriRequest(url, data, false));
    }

    public static String delete(String url) {
        return consumeRequest(new HttpDelete(url));
    }

    public static String delete(String url, Map<String, ?> data) {
        return consumeRequest(buildUriRequest(url, data, true));
    }

    /**
     * post 请求
     * 
     * @param url
     * @param data
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     * @throws UnsupportedOperationException
     */
    public static String post(String url, Map<String, ?> data) {
        return consumeRequest(buildFormRequest(url, data, false));
    }

    public static String post(String url, String json) {
        return consumeRequest(buildJsonRequest(url, json, false));
    }

    public static String put(String url, Map<String, ?> data) {
        return consumeRequest(buildFormRequest(url, data, true));
    }

    public static String put(String url, String json) {
        return consumeRequest(buildJsonRequest(url, json, true));
    }

    public static HttpUriRequest buildUriRequest(String url, Map<String, ?> data, boolean delete) {
        return buildUriRequest(url, data, HTTP.DEF_CONTENT_CHARSET, delete);
    }

    public static HttpUriRequest buildUriRequest(String url, Map<String, ?> data, Charset charset, boolean delete) {
        String uri = buildUri(url, data, charset);
        if (delete) {
            return new HttpDelete(uri);
        }
        return new HttpGet(uri);
    }

    public static String buildUri(String url, Map<String, ?> data) {
        return buildUri(url, data, HTTP.DEF_CONTENT_CHARSET);
    }

    public static String buildUri(String url, Map<String, ?> data, Charset charset) {
        return url + URLEncodedUtils.format(dataToNameValuePairs(data), charset);
    }

    public static HttpUriRequest buildFormRequest(String url, Map<String, ?> data, boolean put) {
        HttpEntityEnclosingRequestBase httpRequest = new HttpPost(url);
        if (put) {
            httpRequest = new HttpPut(url);
        } else {
            httpRequest = new HttpPost(url);
        }
        HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(dataToNameValuePairs(data), "UTF-8");
            httpRequest.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            // never
        }
        return httpRequest;
    }

    public static HttpUriRequest buildJsonRequest(String url, String json, boolean put) {
        HttpEntityEnclosingRequestBase httpRequest = new HttpPost(url);
        if (put) {
            httpRequest = new HttpPut(url);
        } else {
            httpRequest = new HttpPost(url);
        }
        StringEntity entity = new StringEntity(json, "UTF-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httpRequest.setEntity(entity);
        return httpRequest;
    }

    public static void request(String url, Consumer<HttpResponse> consumer) {
        request(new HttpGet(url), consumer);
    }

    public static void request(HttpUriRequest request, Consumer<HttpResponse> consumer) {
        HttpEntity httpEntity = null;
        HttpResponse httpResponse;
        try {
            httpResponse = doRequest(request);
            httpEntity = httpResponse.getEntity();
            if (consumer != null) {
                consumer.accept(httpResponse);
            }
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consume(httpEntity);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 执行请求
     * 
     * @param request
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     */
    public static String consumeRequest(HttpUriRequest request) {
        HttpEntity httpEntity = null;
        try {
            HttpResponse httpResponse = doRequest(request);
            httpEntity = httpResponse.getEntity();
            return EntityUtils.toString(httpEntity);
        } catch (ParseException e) {
            logger.error("http解析响应内容异常:" + e.getMessage(), e);
            throw new RuntimeException("http解析响应内容异常:" + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("http解析响应内容异常:" + e.getMessage(), e);
            throw new RuntimeException("http解析响应内容异常:" + e.getMessage(), e);
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consume(httpEntity);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static HttpResponse doRequest(HttpUriRequest request) {
        if (request instanceof HttpRequestBase) {
            HttpRequestBase httpRequestBase = (HttpRequestBase) request;
            RequestConfig spec = useSpecRequestConfig(request);
            if (spec != null) {
                httpRequestBase.setConfig(spec);
            }
        }
        request.setHeader("connection", "close");
        try {
            return HttpClientHolder.HTTP_CLIENT.execute(request);
        } catch (ClientProtocolException e) {
            logger.error("http请求异常:" + e.getMessage(), e);
            throw new RuntimeException("http请求异常:" + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("http请求异常:" + e.getMessage(), e);
            throw new RuntimeException("http请求异常:" + e.getMessage(), e);
        }
    }

    private static RequestConfig useSpecRequestConfig(HttpUriRequest request) {
        String path = request.getURI().getPath();
        int ldi = path.lastIndexOf("/");
        String methodUri = (ldi >= 0 ? path.substring(ldi + 1) : path);
        String mLowerCase = methodUri.toLowerCase();
        if (mLowerCase.contains("upload") || mLowerCase.contains("download") || mLowerCase.contains("attachment")
                || mLowerCase.startsWith("send")) {
            RequestConfig requestConfig = RequestConfig.custom()
                    // 从连接池中获取连接的超时时间
                    .setConnectionRequestTimeout(2000)
                    // 与服务器连接超时时间：httpclient会创建一个异步线程用以创建socket连接，此处设置该socket的连接超时时间
                    .setConnectTimeout(5000)
                    // socket读数据超时时间：从服务器获取响应数据的超时时间
                    .setSocketTimeout(60000).build();
            return requestConfig;
        }
        return null;
    }
}
