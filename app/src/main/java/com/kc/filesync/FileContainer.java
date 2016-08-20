package com.kc.filesync;

/**
 * Created by Kaustav on 20-Aug-16.
 */
public class FileContainer {

    public FileMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FileMetadata metadata) {
        this.metadata = metadata;
    }

    private FileMetadata metadata;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    private byte[] content;
}
