package com.wifiin.newsay.ai.rag.model;

import java.time.LocalDateTime;
import java.util.Map;

public class Document {
    private String id;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String content;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private Map<String, String> metadata;
    private DocumentStatus status;

    public enum DocumentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public Document() {}

    public Document(String id, String fileName, String fileType) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.status = DocumentStatus.PENDING;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
}
