package pub.carzy.auto_script.db.mapper;

import androidx.databinding.ObservableList;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.Collection;
import java.util.List;

import pub.carzy.auto_script.db.entity.ScriptPointEntity;

/**
 * @author admin
 */
@Dao
public interface ScriptPointMapper extends BaseMapper<ScriptPointEntity> {
    @Query("DELETE FROM script_point WHERE id = :id")
    int deleteById(Long id);

    @Query("DELETE FROM script_point WHERE id IN (:ids)")
    int deleteByIds(Collection<Long> ids);

    @Query("SELECT id FROM script_point WHERE action_id IN (:parentIds)")
    List<Long> findIdByActionIds(Collection<Long> parentIds);

    @Query("select * from script_point where action_id IN (:parentIds)")
    List<ScriptPointEntity> findByActionIds(Collection<Long> parentIds);
    @Query("select * from script_point where script_id IN (:scriptId)")
    List<ScriptPointEntity> findByScriptId(Long scriptId);
    @Query("DELETE FROM script_point WHERE script_id = :id")
    int deleteByScriptId(Long id);
    @Query("DELETE FROM script_point WHERE script_id IN (:ids)")
    void deleteByScriptIds(Collection<Long> ids);
    @Query("SELECT * FROM script_point WHERE script_id IN (:checkedIds)")
    List<ScriptPointEntity> findByScriptIds(Collection<Long> checkedIds);
}
