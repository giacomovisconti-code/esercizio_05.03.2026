package org.example.orderservice.caching;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.orderservice.dto.ProductDto;
import org.example.orderservice.dto.StockRequest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    // configuro e creo la cache
    @Bean
    public CacheManager cacheConfiguration(RedisConnectionFactory cf) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        RedisCacheConfiguration defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues();

        RedisSerializer<ProductDto> productSerializer = new Jackson2JsonRedisSerializer<>(org.example.orderservice.dto.ProductDto.class);

        RedisCacheConfiguration productCfg = defaultCfg.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(productSerializer));

        RedisSerializer<StockRequest> stockSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, StockRequest.class);

        RedisCacheConfiguration stockCfg = defaultCfg.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(stockSerializer));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultCfg)
                .withCacheConfiguration("product", productCfg)
                .withCacheConfiguration("stock", stockCfg)
                .build();
    }
}
