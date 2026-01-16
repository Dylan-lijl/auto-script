package pub.carzy.auto_script.db.mapper;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import pub.carzy.auto_script.db.entity.ScriptEntity;


/**
 * @author admin
 */
@Dao
public interface ScriptMapper extends BaseMapper<ScriptEntity> {

    @Query("DELETE FROM script WHERE id = :id")
    void deleteById(Long id);


    @RawQuery
    List<ScriptEntity> queryList(SupportSQLiteQuery query);

    @Query("DELETE FROM script WHERE id IN (:ids)")
    void deleteByIds(Collection<Long> ids);

    @Query("SELECT id FROM script WHERE id IN (:ids)")
    List<Long> findIdByIds(Set<Long> ids);
}
