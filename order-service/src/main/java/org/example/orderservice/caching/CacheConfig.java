package org.example.orderservice.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.orderservice.dto.ProductDto;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    // configuro la cache
    @Bean
    public RedisCacheConfiguration cacheConfiguration(){

        ObjectMapper mapper = new ObjectMapper();

        return (RedisCacheConfiguration) RedisCacheConfiguration.defaultCacheConfig()
                // setto la scadenza della cache
                .entryTtl(Duration.ofMinutes(60))
                // disabilito il salvataggio in cache dei valori nulli
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(mapper)
                        ));

    }

    // Creo la cache
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory){
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration())
                .build();
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }
}
