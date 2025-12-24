package pub.carzy.auto_script.db.mapper;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;

import java.util.Collection;
import java.util.List;

import pub.carzy.auto_script.db.entity.ScriptActionEntity;

/**
 * @author admin
 */
@Dao
public interface ScriptActionMapper extends BaseMapper<ScriptActionEntity>{
    @Query("DELETE FROM script_action WHERE id = :id")
    int deleteById(Long id);
    @Query("DELETE FROM script_action WHERE id IN (:ids)")
    int deleteByIds(Collection<Long> ids);
    @Query("SELECT id FROM script_action WHERE parent_id = :parentId")
    List<Long> findIdByParentId(Long parentId);
}
