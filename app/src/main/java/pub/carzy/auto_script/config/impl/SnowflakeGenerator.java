package pub.carzy.auto_script.config.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;

/**
 * @author admin
 */
public class SnowflakeGenerator implements IdGenerator<Long> {

    private final Snowflake snowflake;

    public SnowflakeGenerator() {
        int hashCode = UUID.randomUUID().hashCode();
        long nodeId = hashCode & 1023L;
        snowflake = IdUtil.getSnowflake(nodeId & 31L, (nodeId >> 5) & 31L);
    }

    @Override
    public Long nextId() {
        return snowflake.nextId();
    }
}
