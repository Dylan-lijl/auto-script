package pub.carzy.auto_script.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.Executors;

import pub.carzy.auto_script.db.config.DateConverter;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.mapper.ScriptActionMapper;
import pub.carzy.auto_script.db.mapper.ScriptMapper;
import pub.carzy.auto_script.db.mapper.ScriptPointMapper;

/**
 * @author admin
 */
@Database(
        entities = {
                ScriptEntity.class,
                ScriptActionEntity.class,
                ScriptPointEntity.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({
        DateConverter.class
})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ScriptMapper scriptMapper();

    public abstract ScriptActionMapper scriptActionMapper();

    public abstract ScriptPointMapper scriptPointMapper();

    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "app.db"
                            )
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .setQueryCallback(
                                    (sql, bindArgs) -> {
                                        Log.d("ROOM_SQL", "SQL => " + sql);
                                        Log.d("ROOM_SQL", "ARGS => " + bindArgs);
                                    },
                                    Executors.newSingleThreadExecutor()
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

