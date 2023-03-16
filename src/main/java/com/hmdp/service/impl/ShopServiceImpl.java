package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 视频这里用的是原生的逻辑实现
     * 我这里直接想用封装好的框架注解  SpringCache
     * Redis 2.x 版本往后，value与key之间会默认拼上双冒号
     */

    //之前以为控制台输出了sql就以为没有命中缓存，自己方法里加了log来甄别发现是按照预期来的
//    @Cacheable(value = "shop", key = "#id", sync = true)  //这里会解决缓存穿透（因为默认把空缓存了），但是不回解决缓存击穿   我这里手动写加锁+重建逻辑
    @Override
    public Shop queryById(Long id) {
        log.info("没有命中缓存，开始查询~~~~");
//        Shop shop = baseMapper.selectById(id);
//        /**
//         * 反序列化报错：
//         * 要么配 RedisCacheConfiguration
//         * 要么在这里把这个对象里的值全部转成String
//         */
//        Map<String,Object> map = BeanUtil.beanToMap(shop,new HashMap<>(),
//                new CopyOptions().setIgnoreNullValue(true).setFieldValueEditor(((fieldName, fieldValue) -> {
//                    if (fieldValue!=null) {
//                        return fieldValue.toString();
//                    }
//                    return "blank value";
//                })));
//        return map;
        return queryWithPassThrough(id);

    }

    /**
     * 封装缓存穿透代码
     */
    private Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //Redis not exist
        Shop shop = baseMapper.selectById(id);
        //SQL not exist 缓存穿透 -> 缓存空值
        if (shop==null) {
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.SECONDS);
            return null;
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);

        return shop;
    }

    @Override
    @Transactional
    public Long updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return id;
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete("cache:shop"+"::" + id);
        return id;
    }


    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //因为这里自动拆箱有可能null，所以用hutools
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
