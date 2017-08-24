package com.xcz.afcs.biz.http;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.afcs.api.result.BaseResult;
import com.xcz.afcs.api.result.ObjectResult;
import com.xcz.afcs.api.util.ResultValueUtil;
import com.xcz.afcs.core.enums.BaseErrorCode;
import com.xcz.afcs.http.BaseHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.dsig.Reference;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by jingang on 2017/8/24.
 */
public class AfcsHttpClient {

    private static Logger LOG = LoggerFactory.getLogger(AfcsHttpClient.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T get(TypeReference<T> type, String url) {
         return get(type, url, null);
    }

    public static <T> T  get(TypeReference<T> type, String url, Map<String, Object> params) {
        return get(type, url, params, BaseHttpClient.Builder.create());
    }

    public static <T> T  get(TypeReference<T> type, String url, Map<String, Object> params, BaseHttpClient.Builder  builder) {
        String result = BaseHttpClient.get(url, params, builder);
        return convert(type, result, builder);
    }

    public static <T> T post(TypeReference<T> type, String url, Map<String, Object> params) {
        return post(type, url, params, BaseHttpClient.Builder.create());
    }

    public static <T> T post(TypeReference<T> type, String url, Map<String, Object> params, BaseHttpClient.Builder builder) {
        String result = BaseHttpClient.post(url, params, builder);
        return convert(type, result, builder);
    }

    public static <T> T upload(TypeReference<T> type,String url, String name, String filename, InputStream in,
                                Map<String, Object> params, BaseHttpClient.Builder builder) {
        String result = BaseHttpClient.post(url, params, builder);
        return convert(type, result, builder);
    }

    private static <T> T convert(TypeReference<T> type, String result, BaseHttpClient.Builder builder) {
        if (BaseErrorCode.SUCCESS == builder.getResponseCode()) {
            try{
                return mapper.readValue(result, type);
            }catch (Exception e) {
                LOG.error("转换出错", e);
            }
        }
        return null;
    }

}