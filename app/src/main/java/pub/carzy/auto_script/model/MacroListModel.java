package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableList;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.Date;
import java.util.List;

import lombok.Getter;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.DynamicSqlBuilder;
import pub.carzy.auto_script.db.AppDatabase;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MacroListModel extends BaseObservable {
    @Getter
    private final ObservableList<ScriptEntity> data = new ObservableArrayList<>();
    @Getter
    private final ObservableBoolean loading = new ObservableBoolean();
    private String keyword;


    @Bindable
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
        notifyPropertyChanged(BR.keyword);
        reloadData();
    }

    private final AppDatabase database;

    public MacroListModel() {
        super();
        database = BeanFactory.getInstance().get(AppDatabase.class);
    }

    private Date lastTime;
    private Long lastId;
    private int limit = 10;

    @Bindable
    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
        notifyPropertyChanged(BR.limit);
    }

    public void loadData() {
        if (Boolean.TRUE.equals(loading.get())) {
            return;
        }
        ThreadUtil.runOnUi(() -> {
            loading.set(true);
            ThreadUtil.runOnCpu(() -> {
                List<ScriptEntity> list = database.scriptMapper().queryList(
                        DynamicSqlBuilder.queryByCursorOrderByUpdateTimeDesc(
                                keyword, lastTime, lastId, limit
                        )
                );
                ThreadUtil.runOnUi(() -> {
                    if (!list.isEmpty()) {
                        ScriptEntity last = list.get(list.size() - 1);
                        lastTime = last.getUpdateTime();
                        lastId = last.getId();
                        data.addAll(list);
                    }
                    loading.set(false);
                });
            });
        });

    }


    public void reloadData() {
        if (loading.get()) {
            return;
        }
        lastTime = null;
        lastId = null;
        ThreadUtil.runOnUi(() -> {
            data.clear();
            loadData();
        });
    }
}
