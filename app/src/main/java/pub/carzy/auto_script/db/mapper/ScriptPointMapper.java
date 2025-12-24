package pub.carzy.auto_script.db.mapper;

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
    @Query("SELECT id FROM script_point WHERE parent_id = :parentId")
    List<Long> findIdByParentId(Long parentId);
    @Query("SELECT id FROM script_point WHERE parent_id IN (:parentIds)")
    List<Long> findIdByParentIds(List<Long> parentIds);
}
