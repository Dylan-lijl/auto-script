package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Date;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.utils.ActivityUtils;

/**
 * @author admin
 */
public class AboutBasicInfoModel extends BaseObservable {
    private Integer minSdk;
    private Integer targetSdk;
    private Integer currentSdk;
    private String sourceRepository;
    private String version;
    private Date updateTime;

    @Bindable
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        notifyPropertyChanged(BR.updateTime);
    }

    @Bindable
    public String getMinSdkName() {
        return ActivityUtils.getApiName(minSdk);
    }

    @Bindable
    public String getTargetSdkName() {
        return ActivityUtils.getApiName(targetSdk);
    }

    @Bindable
    public String getCurrentSdkName() {
        return ActivityUtils.getApiName(currentSdk);
    }

    @Bindable
    public Integer getMinSdk() {
        return minSdk;
    }

    public void setMinSdk(Integer minSdk) {
        this.minSdk = minSdk;
        notifyPropertyChanged(BR.minSdk);
        notifyPropertyChanged(BR.minSdkName);
    }

    @Bindable
    public Integer getTargetSdk() {
        return targetSdk;
    }

    public void setTargetSdk(Integer targetSdk) {
        this.targetSdk = targetSdk;
        notifyPropertyChanged(BR.targetSdk);
        notifyPropertyChanged(BR.targetSdkName);
    }

    @Bindable
    public Integer getCurrentSdk() {
        return currentSdk;
    }

    public void setCurrentSdk(Integer currentSdk) {
        this.currentSdk = currentSdk;
        notifyPropertyChanged(BR.currentSdk);
        notifyPropertyChanged(BR.currentSdkName);
    }

    @Bindable
    public String getSourceRepository() {
        return sourceRepository;
    }

    public void setSourceRepository(String sourceRepository) {
        this.sourceRepository = sourceRepository;
        notifyPropertyChanged(BR.sourceRepository);
    }

    @Bindable
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
        notifyPropertyChanged(BR.version);
    }
}
