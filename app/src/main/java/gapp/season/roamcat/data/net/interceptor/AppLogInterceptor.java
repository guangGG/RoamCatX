package gapp.season.roamcat.data.net.interceptor;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import gapp.season.util.log.LogUtil;
import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;

public final class AppLogInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private volatile Level mPrintLevel = Level.NONE;
    private String mTag;

    public enum Level {
        NONE,
        BASIC, //only print request/response first line
        HEADERS,
        BODY,
        PROTECTION //print all without printing protected data
    }

    public AppLogInterceptor(String tag) {
        mTag = tag;
    }

    public AppLogInterceptor setPrintLevel(Level level) {
        if (level == null) throw new NullPointerException();
        mPrintLevel = level;
        return this;
    }

    private void log(String message, int level) {
        switch (level) {
            case Log.VERBOSE:
                LogUtil.v(mTag, message);
                break;
            case Log.DEBUG:
                LogUtil.d(mTag, message);
                break;
            case Log.WARN:
                LogUtil.w(mTag, message);
                break;
            case Log.ERROR:
                LogUtil.e(mTag, message);
                break;
            case Log.INFO:
            default:
                LogUtil.i(mTag, message);
                break;
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (mPrintLevel == Level.NONE) {
            return chain.proceed(request);
        }

        StringBuilder logMessage = new StringBuilder();
        // print request logs
        logMessage.append(logForRequest(request, chain.connection()))
                .append("\n");

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logMessage.append("<-- HTTP FAILED: ")
                    .append(e);
            log(logMessage.toString(), Log.INFO);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        // print response logs
        ResponseWithLog responseWithLog = logForResponse(response, tookMs);
        logMessage.append(responseWithLog.message);
        log(logMessage.toString(), Log.INFO);
        return responseWithLog.response;
    }

    private String logForRequest(Request request, Connection connection) {
        boolean logProtection = mPrintLevel == Level.PROTECTION;
        boolean logBody = (mPrintLevel == Level.BODY || mPrintLevel == Level.PROTECTION);
        boolean logHeaders = (mPrintLevel == Level.BODY || mPrintLevel == Level.PROTECTION || mPrintLevel == Level.HEADERS);
        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        StringBuilder logMessage = new StringBuilder();

        try {
            logMessage.append("--> ")
                    .append(request.method())
                    .append(" ")
                    .append(request.url())
                    .append(" ")
                    .append(protocol)
                    .append("\n");

            if (logHeaders) {
                if (hasRequestBody) {
                    // Request body headers are only present when installed as a network interceptor. Force
                    // them to be included (when available) so there values are known.
                    if (requestBody.contentType() != null) {
                        logMessage.append("\tContent-Type: ")
                                .append(requestBody.contentType())
                                .append("\n");
                    }
                    if (requestBody.contentLength() != -1) {
                        logMessage.append("\tContent-Length: ")
                                .append(requestBody.contentLength())
                                .append("\n");
                    }
                }

                boolean breakLine = false;
                Headers headers = request.headers();
                for (int i = 0, count = headers.size(); i < count; i++) {
                    String name = headers.name(i);
                    if (logProtection && "Cookie".equalsIgnoreCase(name)) {
                        logMessage.append("\t")
                                .append(name)
                                .append(": ******")
                                .append("\n");
                        if (!breakLine) {
                            breakLine = true;
                        }
                        continue;
                    }
                    // Skip headers from the request body as they are explicitly logged above.
                    if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                        logMessage.append("\t")
                                .append(name)
                                .append(": ")
                                .append(headers.value(i))
                                .append("\n");
                        if (!breakLine) {
                            breakLine = true;
                        }
                    }
                }
                if (breakLine) {
                    logMessage.append("\t***\n");
                }

                if (logBody && hasRequestBody) {
                    if (isPlaintext(requestBody.contentType())) {
                        String body = bodyToString(request);
                        if (!TextUtils.isEmpty(body)) {
                            logMessage.append(body)
                                    .append("\n");
                        }
                    } else {
                        logMessage.append("\tbody: maybe [binary body], omitted!")
                                .append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log(e.getMessage(), Log.ERROR);
        } finally {
            logMessage.append("--> END ")
                    .append(request.method());
        }
        return logMessage.toString();
    }

    private ResponseWithLog logForResponse(Response response, long tookMs) {
        ResponseWithLog result = new ResponseWithLog();
        result.response = response;

        Response.Builder builder = response.newBuilder();
        Response clone = builder.build();
        ResponseBody responseBody = clone.body();
        boolean logProtection = mPrintLevel == Level.PROTECTION;
        boolean logBody = (mPrintLevel == Level.BODY || mPrintLevel == Level.PROTECTION);
        boolean logHeaders = (mPrintLevel == Level.BODY || mPrintLevel == Level.PROTECTION || mPrintLevel == Level.HEADERS);
        StringBuilder logMessage = new StringBuilder();

        try {
            logMessage.append("<-- ")
                    .append(clone.code())
                    .append(" ")
                    .append(clone.message())
                    .append(" ")
                    .append(clone.request().url())
                    .append(" (")
                    .append(tookMs)
                    .append("ms）")
                    .append("\n");
            if (logHeaders) {
                boolean breakLine = false;
                Headers headers = clone.headers();
                for (int i = 0, count = headers.size(); i < count; i++) {
                    String name = headers.name(i);
                    if (logProtection && "Cookie".equalsIgnoreCase(name)) {
                        logMessage.append("\t")
                                .append(headers.name(i))
                                .append(": ******")
                                .append("\n");
                        if (!breakLine) {
                            breakLine = true;
                        }
                        continue;
                    }

                    logMessage.append("\t")
                            .append(headers.name(i))
                            .append(": ")
                            .append(headers.value(i))
                            .append("\n");
                    if (!breakLine) {
                        breakLine = true;
                    }
                }
                if (breakLine) {
                    logMessage.append("\t***\n");
                }

                if (logBody && HttpHeaders.hasBody(clone)) {
                    if (responseBody != null) {
                        //add isShortContent verification in case Content-Type error
                        if (isPlaintext(responseBody.contentType()) || isShortContent(responseBody.contentLength())) {
                            byte[] bytes = toByteArray(responseBody.byteStream());
                            MediaType contentType = responseBody.contentType();
                            String body = new String(bytes, getCharset(contentType));
                            logMessage.append("\tbody:")
                                    .append(body)
                                    .append("\n");
                            responseBody = ResponseBody.create(responseBody.contentType(), bytes);
                            result.response = response.newBuilder().body(responseBody).build();
                        } else {
                            logMessage.append("\tbody: maybe [binary body], omitted!")
                                    .append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log(e.getMessage(), Log.ERROR);
        } finally {
            logMessage.append("<-- END HTTP");
        }
        result.message = logMessage.toString();
        return result;
    }

    private boolean isShortContent(long contentLength) {
        return contentLength > 0 && contentLength < 4096;
    }

    private Charset getCharset(MediaType contentType) {
        Charset charset = contentType != null ? contentType.charset(UTF8) : UTF8;
        if (charset == null) charset = UTF8;
        return charset;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    public static boolean isPlaintext(MediaType mediaType) {
        if (mediaType == null) return false;
        if ("text".equals(mediaType.type())) {
            return true;
        }
        String subtype = mediaType.subtype();
        if (!TextUtils.isEmpty(subtype)) {
            subtype = subtype.toLowerCase();
            return subtype.contains("x-www-form-urlencoded") || subtype.contains("json")
                    || subtype.contains("xml") || subtype.contains("html");
        }
        return false;
    }

    private String bodyToString(Request request) {
        try {
            Request copy = request.newBuilder().build();
            RequestBody body = copy.body();
            if (body == null) return null;
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            Charset charset = getCharset(body.contentType());
            return "\tbody:" + buffer.readString(charset);
        } catch (Exception e) {
            log(e.getMessage(), Log.ERROR);
            return null;
        }
    }

    private byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(input, output);
        output.close();
        return output.toByteArray();
    }

    private void write(InputStream inputStream, OutputStream outputStream) throws IOException {
        int len;
        byte[] buffer = new byte[4096];
        while ((len = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, len);
    }

    private static final class ResponseWithLog {
        private Response response;
        private String message;
    }
}
