package com.canteen.config;

import com.canteen.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 数据库连通验证器（临时工具）
 * 仅当启动参数带 --canteen.db-verify=true 时执行：验证 Mapper -> MyBatis -> MySQL 链路
 * 验证通过后此组件无副作用（正常启动不会触发）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "canteen.db-verify", havingValue = "true")
public class DbVerifyRunner implements CommandLineRunner {

    private final UserMapper userMapper;

    public DbVerifyRunner(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        long count = userMapper.countAll();
        log.info("[DB-VERIFY] 数据库连通验证通过：user 表当前记录数 = {}", count);
        System.out.println("[DB-VERIFY] 数据库连通验证通过：user 表当前记录数 = " + count);
    }
}
