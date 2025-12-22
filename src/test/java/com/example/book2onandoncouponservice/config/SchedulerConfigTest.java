package com.example.book2onandoncouponservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@ExtendWith(MockitoExtension.class)
class SchedulerConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @InjectMocks
    private SchedulerConfig schedulerConfig;

    @Test
    @DisplayName("LockProvider Bean 생성 확인")
    void lockProvider() {
        // when
        LockProvider lockProvider = schedulerConfig.lockProvider(redisConnectionFactory);

        // then
        assertThat(lockProvider).isNotNull();
        assertThat(lockProvider).isInstanceOf(RedisLockProvider.class);
    }
}