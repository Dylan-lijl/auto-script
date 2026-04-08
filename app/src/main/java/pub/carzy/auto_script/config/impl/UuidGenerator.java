package pub.carzy.auto_script.config.impl;

import java.util.UUID;

import pub.carzy.auto_script.config.IdGenerator;

/**
 * uuid实现
 * @author admin
 */
public class UuidGenerator implements IdGenerator<String> {
    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
