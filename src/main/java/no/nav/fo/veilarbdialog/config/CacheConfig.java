package no.nav.fo.veilarbdialog.config;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static net.sf.ehcache.config.PersistenceConfiguration.Strategy.LOCALTEMPSWAP;
import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final CacheConfiguration FNR_FROM_AKTOR_ID_CACHE = new CacheConfiguration("fnrFromAktorId", 100000)
            .memoryStoreEvictionPolicy(LRU)
            .timeToIdleSeconds(86400)
            .timeToLiveSeconds(86400)
            .persistence(new PersistenceConfiguration().strategy(LOCALTEMPSWAP));

    private static final CacheConfiguration AKTOR_ID_FROM_FNR_CACHE = new CacheConfiguration("aktorIdFromFnr", 100000)
            .memoryStoreEvictionPolicy(LRU)
            .timeToIdleSeconds(86400)
            .timeToLiveSeconds(86400)
            .persistence(new PersistenceConfiguration().strategy(LOCALTEMPSWAP));

    private static final CacheConfiguration ABAC_CACHE = new CacheConfiguration("askForPermission", 10000)
            .memoryStoreEvictionPolicy(LRU)
            .timeToIdleSeconds(3600)
            .timeToLiveSeconds(3600);

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(FNR_FROM_AKTOR_ID_CACHE);
        config.addCache(AKTOR_ID_FROM_FNR_CACHE);
        config.addCache(ABAC_CACHE);
        return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }

}
