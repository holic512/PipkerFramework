/**
 * @file BoundedResponseCaptureWrapper.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 在不延迟响应写出的前提下，最多缓存指定字节数的 HTTP 响应副本。
 * @logic 输出流始终直接委托给原响应，同时向内存副本写入受限数据，避免 ContentCachingResponseWrapper 的无限缓存风险。
 * @dependencies Jakarta Servlet API、Java I/O
 * @index_tags log、http、response-body
 * @author holic512
 */
package com.pipker.starter.log.web;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 在响应正常写出的同时，按字节数限制捕获响应副本。
 */
final class BoundedResponseCaptureWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capturedContent = new ByteArrayOutputStream();
    private final int maxLength;

    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean truncated;

    /**
     * 创建受限响应包装器。
     *
     * @param response 原始 HTTP 响应
     * @param maxLength 最多捕获的字节数
     */
    BoundedResponseCaptureWrapper(HttpServletResponse response, int maxLength) {
        super(response);
        this.maxLength = Math.max(0, maxLength);
    }

    /**
     * 返回会同时写入原响应和受限缓存的输出流。
     *
     * @return 响应输出流
     * @throws IOException 无法获取原响应输出流时抛出
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CapturingServletOutputStream(super.getOutputStream());
        }
        return outputStream;
    }

    /**
     * 返回使用响应字符集并连接到受限输出流的字符写入器。
     *
     * @return 响应写入器
     * @throws IOException 无法获取原响应输出流时抛出
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            Charset charset = resolveCharset();
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset), false);
        }
        return writer;
    }

    /**
     * 先刷新包装器写入器，再刷新原响应缓冲区。
     *
     * @throws IOException 刷新原响应失败时抛出
     */
    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        super.flushBuffer();
    }

    /**
     * 重置原响应及已捕获内容。
     */
    @Override
    public void reset() {
        super.reset();
        capturedContent.reset();
        truncated = false;
    }

    /**
     * 重置原响应缓冲区及已捕获内容。
     */
    @Override
    public void resetBuffer() {
        super.resetBuffer();
        capturedContent.reset();
        truncated = false;
    }

    /**
     * 返回当前已捕获的响应内容。
     *
     * @return 响应内容字节数组
     */
    byte[] getCapturedContent() {
        if (writer != null) {
            writer.flush();
        }
        return capturedContent.toByteArray();
    }

    /**
     * 判断响应内容是否超过捕获上限。
     *
     * @return 超过上限时返回 {@code true}
     */
    boolean isTruncated() {
        return truncated;
    }

    private Charset resolveCharset() {
        String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 仅将尚未超过上限的字节复制到内存缓存，并记录是否发生截断。
     */
    private void capture(byte[] bytes, int offset, int length) {
        int remaining = maxLength - capturedContent.size();
        if (remaining <= 0) {
            truncated = truncated || length > 0;
            return;
        }
        int capturedLength = Math.min(remaining, length);
        capturedContent.write(bytes, offset, capturedLength);
        truncated = truncated || capturedLength < length;
    }

    /**
     * 将写入委托给原输出流并同步捕获有限副本的输出流。
     */
    private final class CapturingServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        private CapturingServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        /**
         * 写入单个字节并尝试捕获该字节。
         *
         * @param value 待写入的字节
         * @throws IOException 原响应写入失败时抛出
         */
        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            capture(new byte[]{(byte) value}, 0, 1);
        }

        /**
         * 写入字节数组并尝试捕获其中的有限部分。
         *
         * @param bytes 待写入的字节数组
         * @param offset 起始偏移量
         * @param length 写入长度
         * @throws IOException 原响应写入失败时抛出
         */
        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            capture(bytes, offset, length);
        }

        /**
         * 返回原输出流的就绪状态。
         *
         * @return 原输出流是否就绪
         */
        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        /**
         * 将写入监听器委托给原输出流。
         *
         * @param writeListener 写入监听器
         */
        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
    }
}
