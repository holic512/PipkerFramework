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

final class BoundedResponseCaptureWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capturedContent = new ByteArrayOutputStream();
    private final int maxLength;

    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean truncated;

    BoundedResponseCaptureWrapper(HttpServletResponse response, int maxLength) {
        super(response);
        this.maxLength = Math.max(0, maxLength);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CapturingServletOutputStream(super.getOutputStream());
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            Charset charset = resolveCharset();
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset), false);
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        super.flushBuffer();
    }

    @Override
    public void reset() {
        super.reset();
        capturedContent.reset();
        truncated = false;
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        capturedContent.reset();
        truncated = false;
    }

    byte[] getCapturedContent() {
        if (writer != null) {
            writer.flush();
        }
        return capturedContent.toByteArray();
    }

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

    private final class CapturingServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        private CapturingServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            capture(new byte[]{(byte) value}, 0, 1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            capture(bytes, offset, length);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
    }
}
