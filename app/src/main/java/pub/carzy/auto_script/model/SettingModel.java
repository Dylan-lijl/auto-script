package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.entity.SettingProxy;

/**
 * @author admin
 */
public class SettingModel extends BaseObservable {
    private SettingProxy proxy;

    @Bindable
    public SettingProxy getProxy() {
        return proxy;
    }

    public void setProxy(SettingProxy proxy) {
        this.proxy = proxy;
        notifyPropertyChanged(BR.proxy);
    }
}
