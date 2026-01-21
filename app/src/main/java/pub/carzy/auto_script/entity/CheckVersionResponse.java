package pub.carzy.auto_script.entity;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * @author admin
 */
@Data
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

    @Data
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
    }

    @Data
    public static class Uploader {
        private String login;
        private Long id;
        @SerializedName("node_id")
        private String nodeId;
        @SerializedName("html_url")
        private String htmlUrl;
    }
}
