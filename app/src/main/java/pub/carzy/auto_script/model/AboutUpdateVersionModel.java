package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Locale;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class AboutUpdateVersionModel extends BaseObservable {
    private String name;
    private String appFileName;
    private String tagName;
    private String body;
    private String login;
    private String htmlUrl;
    private String browserDownloadUrl;
    private String contentType;
    private Integer size;
    private String digest;
    private String label;
    private String url;
    private Integer downloadCount;
    private boolean show = false;

    @Bindable
    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
        notifyPropertyChanged(BR.show);
    }

    @Bindable
    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
        notifyPropertyChanged(BR.downloadCount);
    }

    @Bindable
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        notifyPropertyChanged(BR.url);
    }

    @Bindable
    public String getAppFileName() {
        return appFileName;
    }

    public void setAppFileName(String appFileName) {
        this.appFileName = appFileName;
        notifyPropertyChanged(BR.appFileName);
    }

    @Bindable
    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        notifyPropertyChanged(BR.updatedAt);
    }

    @Bindable
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        notifyPropertyChanged(BR.label);
    }

    @Bindable
    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
        notifyPropertyChanged(BR.digest);
    }

    @Bindable
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
        notifyPropertyChanged(BR.size);
        notifyPropertyChanged(BR.kbSize);
        notifyPropertyChanged(BR.mbSize);
    }
    @Bindable
    public String getKbSize(){
        if (size == null) {
            return null;
        }
        return String.format(Locale.getDefault(),"%.2f ", size / 1024f );
    }
    @Bindable
    public String getMbSize(){
        if (size == null) {
            return null;
        }
        return String.format(Locale.getDefault(),"%.2f ", size / 1024f / 1024f);
    }
    @Bindable
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
        notifyPropertyChanged(BR.contentType);
    }

    @Bindable
    public String getBrowserDownloadUrl() {
        return browserDownloadUrl;
    }

    public void setBrowserDownloadUrl(String browserDownloadUrl) {
        this.browserDownloadUrl = browserDownloadUrl;
        notifyPropertyChanged(BR.browserDownloadUrl);
    }

    @Bindable
    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        notifyPropertyChanged(BR.htmlUrl);
    }

    @Bindable
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
        notifyPropertyChanged(BR.login);
    }

    @Bindable
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        notifyPropertyChanged(BR.body);
    }

    @Bindable
    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
        notifyPropertyChanged(BR.tagName);
    }

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }
    private Date updatedAt;

}
