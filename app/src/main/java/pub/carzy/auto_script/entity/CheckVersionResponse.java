package pub.carzy.auto_script.entity;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;


/**
 * @author admin
 */
public class CheckVersionResponse {
    private String name;
    private String message;
    @SerializedName("documentation_url")
    private String documentationUrl;
    private String status;
    @SerializedName("tag_name")
    private String tagName;
    @SerializedName("published_at")
    private Date publishedAt;
    private List<Asset> assets;
    private String body;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static class Asset {
        private String url;
        private Long id;
        @SerializedName("node_id")
        private String nodeId;
        private String name;
        private String label;
        @SerializedName("content_type")
        private String contentType;
        private Integer size;
        private String digest;
        @SerializedName("created_at")
        private Date createdAt;
        @SerializedName("updated_at")
        private Date updatedAt;
        @SerializedName("browser_download_url")
        private String browserDownloadUrl;
        private Uploader uploader;
        @SerializedName("download_count")
        private Integer downloadCount;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public String getDigest() {
            return digest;
        }

        public void setDigest(String digest) {
            this.digest = digest;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getBrowserDownloadUrl() {
            return browserDownloadUrl;
        }

        public void setBrowserDownloadUrl(String browserDownloadUrl) {
            this.browserDownloadUrl = browserDownloadUrl;
        }

        public Uploader getUploader() {
            return uploader;
        }

        public void setUploader(Uploader uploader) {
            this.uploader = uploader;
        }

        public Integer getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(Integer downloadCount) {
            this.downloadCount = downloadCount;
        }
    }

    public static class Uploader {
        private String login;
        private Long id;
        @SerializedName("node_id")
        private String nodeId;
        @SerializedName("html_url")
        private String htmlUrl;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }
    }
}
