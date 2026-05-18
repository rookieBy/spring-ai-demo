package com.wifiin.newsay.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.ai.vectorstore.milvus")
public class MilvusProperties {
    private String host = "localhost";
    private int port = 19530;
    private String user;
    private String password;
    private String collectionPrefix = "rag_";

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCollectionPrefix() { return collectionPrefix; }
    public void setCollectionPrefix(String collectionPrefix) { this.collectionPrefix = collectionPrefix; }
}
