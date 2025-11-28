package pub.carzy.auto_script.config.impl;

import java.util.UUID;

import cn.hutool.core.util.IdUtil;
import pub.carzy.auto_script.config.IdGenerator;

/**
 * @author admin
 */
public class UuidGenerator implements IdGenerator<String> {
    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
