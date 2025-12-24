package pub.carzy.auto_script.config;

import android.text.TextUtils;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author admin
 */
public class DynamicSqlBuilder {
    public static SupportSQLiteQuery queryByCursorOrderByUpdateTimeDesc(
            String keyword,
            Date lastTime,
            Long lastId,
            int limit
    ) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();
        sql.append("SELECT * FROM script WHERE 1=1 ");

        if (!TextUtils.isEmpty(keyword)) {
            sql.append("AND name LIKE ? ");
            args.add("%" + keyword + "%");
        }
        if (lastTime != null && lastId != null) {
            sql.append("AND (update_time < ? OR (update_time = ? AND id < ?))");
            args.add(lastTime);
            args.add(lastTime);
            args.add(lastId);
        }
        sql.append("ORDER BY update_time DESC, id DESC ");
        sql.append("LIMIT ? ");
        args.add(limit);
        return new SimpleSQLiteQuery(sql.toString(), args.toArray());
    }
}
