package pub.carzy.auto_script.db.mapper;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Upsert;

import java.util.Collection;

/**
 * @author admin
 */
public interface BaseMapper<T> {
    @Upsert
    void save(T entity);

    @Upsert
    void save(Collection<T> entity);

    @Insert
    long insert(T entity);

    @Insert
    long[] insert(Collection<T> entity);

    @Delete
    int delete(T entity);

    @Update
    int update(T entity);
}
