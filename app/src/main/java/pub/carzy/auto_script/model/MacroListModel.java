package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;
import androidx.databinding.ObservableList;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.DynamicSqlBuilder;
import pub.carzy.auto_script.db.AppDatabase;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.statics.StaticValues;

/**
 * @author admin
 */
public class MacroListModel extends BaseObservable {
    @Getter
    private final ObservableList<ScriptEntity> data = new ObservableArrayList<>();
    @Getter
    private final ObservableBoolean loading = new ObservableBoolean();
    @Getter
    private final ObservableList<Long> deleteIds = new ObservableArrayList<>();
    private String keyword;


    @Bindable
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
        notifyPropertyChanged(BR.keyword);
    }

    private final AppDatabase database;

    public MacroListModel() {
        super();
        database = BeanFactory.getInstance().get(AppDatabase.class);
    }

    private final AtomicReference<Date> lastTime = new AtomicReference<>();
    private final AtomicReference<Long> lastId = new AtomicReference<>(null);
    private final AtomicInteger limit = new AtomicInteger(10);

    @Bindable
    public int getLimit() {
        return limit.get();
    }

    public void setLimit(int limit) {
        this.limit.set(limit);
        notifyPropertyChanged(BR.limit);
    }

    public void loadData() {
        requestData(null);
    }

    public void requestData(Runnable runnable) {
        if (Boolean.TRUE.equals(loading.get())) {
            return;
        }
        ThreadUtil.runOnUi(() -> {
            if (Boolean.TRUE.equals(loading.get())) {
                return;
            }
            if (runnable != null) {
                runnable.run();
            }
            loading.set(true);
            ThreadUtil.runOnCpu(() -> {
                List<ScriptEntity> list = database.scriptMapper().queryList(
                        DynamicSqlBuilder.queryByCursorOrderByUpdateTimeDesc(
                                keyword, lastTime.get(), lastId.get(), limit.get()
                        )
                );
                ThreadUtil.runOnUi(() -> {
                    if (!list.isEmpty()) {
                        ScriptEntity last = list.get(list.size() - 1);
                        lastTime.set(last.getUpdateTime());
                        lastId.set(last.getId());
                        data.addAll(list);
                    }
                    loading.set(false);
                });
            });
        });
    }


    public void reloadData() {
        requestData(() -> {
            lastTime.set(null);
            lastId.set(null);
            data.clear();
            loadData();
        });
    }
}
