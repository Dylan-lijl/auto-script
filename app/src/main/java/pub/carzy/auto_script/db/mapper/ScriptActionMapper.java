package pub.carzy.auto_script.db.mapper;

import androidx.databinding.ObservableList;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.Collection;
import java.util.List;

import pub.carzy.auto_script.db.entity.ScriptActionEntity;

/**
 * @author admin
 */
@Dao
public interface ScriptActionMapper extends BaseMapper<ScriptActionEntity> {
    @Query("DELETE FROM script_action WHERE id = :id")
    int deleteById(Long id);

    @Query("DELETE FROM script_action WHERE id IN (:ids)")
    int deleteByIds(Collection<Long> ids);

    @Query("SELECT id FROM script_action WHERE script_id = :parentId")
    List<Long> findIdByScriptId(Long parentId);

    @Query("select * from script_action where script_id = :parentId")
    List<ScriptActionEntity> findByScriptId(Long parentId);
    @Query("DELETE FROM script_action WHERE script_id = :id")
    int deleteByScriptId(Long id);

    @Query("DELETE FROM script_action WHERE script_id IN (:ids)")
    void deleteByScriptIds(Collection<Long> ids);
    @Query("SELECT * FROM script_action WHERE script_id IN (:checkedIds)")
    List<ScriptActionEntity> findByScriptIds(Collection<Long> checkedIds);
}
