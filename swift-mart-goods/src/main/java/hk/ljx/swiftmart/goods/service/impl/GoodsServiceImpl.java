package hk.ljx.swiftmart.goods.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import hk.ljx.swiftmart.common.constant.RedisKeyConstants;
import hk.ljx.swiftmart.common.domain.dataobject.*;
import hk.ljx.swiftmart.common.domain.mapper.*;
import hk.ljx.swiftmart.common.enums.ActivityStatusEnum;
import hk.ljx.swiftmart.common.enums.ResponseCodeEnum;
import hk.ljx.swiftmart.common.exception.BizException;
import hk.ljx.swiftmart.common.utils.JsonUtils;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsDetailReqVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsDetailRspVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListReqVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListRspVO;
import hk.ljx.swiftmart.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static hk.ljx.swiftmart.common.constant.RedisKeyConstants.GOODS_DETAIL_PREFIX;
import static hk.ljx.swiftmart.common.constant.RedisKeyConstants.GOODS_DETAIL_TTL_MINUTES;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Resource
    private GoodsImgDOMapper goodsImgDOMapper;

    @Resource
    private GoodsDetailDOMapper goodsDetailDOMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 查询秒杀商品列表
     *
     * @param reqVO
     * @return
     */
    @Override
    public Response<List<FindSeckillGoodsListRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO) {
        Long activityId = reqVO.getActivityId();
        log.info("查询秒杀商品列表,活动ID:{}", activityId);

        // 构建 Redis 缓存 Key
        String redisKey = RedisKeyConstants.GOODS_LIST_PREFIX + activityId;

        // 先查 Redis 缓存
        String redisJsonValue = stringRedisTemplate.opsForValue().get(redisKey);

        // 缓存不为空
        if (StrUtil.isNotBlank(redisJsonValue)) {
            log.info("==> 命中商品列表缓存, redisKey: {}", redisKey);
            // 手动将 String 字符串，反序列化为商品列表
            List<FindSeckillGoodsListRspVO> cachedList = JsonUtils
                    .parseArray(redisJsonValue, FindSeckillGoodsListRspVO.class);
            // 设置库存字段值
            supplementStock(cachedList, activityId);
            // 实时重新计算活动状态
            FindSeckillGoodsListRspVO first = cachedList.get(0);
            ActivityStatusEnum activityStatusEnum = calculateActivityStatus(first.getBeginTime(), first.getEndTime());
            cachedList.forEach(item ->
                    item.setActivityStatus(activityStatusEnum.getStatus()));

            return Response.success(cachedList);
        }

        // 查询活动消息
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        // 根据活动id查询商品信息
        List<SeckillGoodsDO> seckillGoodsDOList = seckillGoodsDOMapper.selectByActivityId(activityId);
        if (CollUtil.isEmpty(seckillGoodsDOList)) {
            log.info("活动ID:{},没有关联商品", activityId);
            return Response.success(Collections.emptyList());
        }
        // 查询关联的商品信息 获取原价
        List<Long> goodsIds = seckillGoodsDOList.stream().map(SeckillGoodsDO::getGoodsId).toList();
        // 一次批量查询所有商品
        List<GoodsDO> goodsDOS = goodsDOMapper.selectByIds(goodsIds);
        // 将商品和 id 映射为 Map 方便查询
        Map<Long, GoodsDO> goodsDOMap = goodsDOS.stream().collect(Collectors.toMap(GoodsDO::getId, goods -> goods));
        // 计算活动状态
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO.getBeginTime(), activityDO.getEndTime());
        // 组装响应数据
        ArrayList<FindSeckillGoodsListRspVO> listRspVOS = new ArrayList<>();
        for (SeckillGoodsDO seckillGoodsDO : seckillGoodsDOList) {
            FindSeckillGoodsListRspVO goodsListRspVO = new FindSeckillGoodsListRspVO();
            goodsListRspVO.setId(seckillGoodsDO.getId());
            goodsListRspVO.setActivityId(seckillGoodsDO.getActivityId());
            goodsListRspVO.setSeckillTitle(seckillGoodsDO.getSeckillTitle());
            goodsListRspVO.setSeckillImg(seckillGoodsDO.getSeckillImg());
            goodsListRspVO.setSeckillPrice(seckillGoodsDO.getSeckillPrice());
            goodsListRspVO.setSeckillTotal(seckillGoodsDO.getSeckillTotal());
            goodsListRspVO.setSeckillStock(seckillGoodsDO.getSeckillStock());
            goodsListRspVO.setActivityStatus(activityStatusEnum.getStatus());
            goodsListRspVO.setBeginTime(activityDO.getBeginTime());
            goodsListRspVO.setEndTime(activityDO.getEndTime());
            goodsListRspVO.setGoodsId(seckillGoodsDO.getGoodsId());

            GoodsDO goodsDO = goodsDOMap.get(seckillGoodsDO.getGoodsId());
            if (Objects.nonNull(goodsDO)) {
                goodsListRspVO.setGoodsPrice(goodsDO.getGoodsPrice());
            }
            listRspVOS.add(goodsListRspVO);
        }

        // 将商品列表写入 Redis 缓存
        log.info("==> 商品列表缓存未命中，将数据写入 Redis, redisKey: {}", redisKey);
        Long endTime = RedisKeyConstants.calculateTtlSeconds(activityDO.getEndTime());
        if (Objects.nonNull(endTime) && endTime > 0) {
            // 活动未结束
            stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(listRspVOS), endTime, TimeUnit.SECONDS);
        } else {
          // 活动结束，余温缓存
            stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(listRspVOS), RedisKeyConstants.ENDED_ACTIVITY_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return Response.success(listRspVOS);
    }

    /**
     * 实时补充库存字段
     * @param goodsList
     * @param activityId
     */
    private void supplementStock(List<FindSeckillGoodsListRspVO> goodsList, Long activityId) {
        // 根据活动 ID 查询秒杀商品的实时库存（仅查 id 和 seckill_stock 字段，减少 IO 开销）
        List<SeckillGoodsDO> seckillGoodsDOS = seckillGoodsDOMapper.selectStockByActivityId(activityId);

        // 构建 ID -> 库存的映射
        Map<Long, Integer> stockMap = seckillGoodsDOS.stream()
                .collect(Collectors.toMap(SeckillGoodsDO::getId, SeckillGoodsDO::getSeckillStock));

        // 补充库存到缓存中的商品列表
        for (FindSeckillGoodsListRspVO rspVO : goodsList) {
            Integer stock = stockMap.get(rspVO.getId());
            if (Objects.nonNull(stock)) {
                rspVO.setSeckillStock(stock);
            }
        }
    }

    @Override
    public Response<FindSeckillGoodsDetailRspVO> findSeckillGoodsDetail(FindSeckillGoodsDetailReqVO reqVO) {
        Long goodsId = reqVO.getGoodsId();
        Long activityId = reqVO.getActivityId();
        log.info("查询秒杀商品详情,商品ID:{},活动ID:{}", goodsId, activityId);

        String key = GOODS_DETAIL_PREFIX + activityId + ":" + goodsId;
        String goodsJson = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(goodsJson)) {
            // 命中缓存
            FindSeckillGoodsDetailRspVO goods = JsonUtils.parseObject(goodsJson, FindSeckillGoodsDetailRspVO.class);
            SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectStockByActivityIdAndGoodsId(activityId, goodsId);
            if (Objects.nonNull(seckillGoodsDO)) {
                goods.setSeckillStock(seckillGoodsDO.getSeckillStock());
            }
            // 实时重新计算活动状态
            ActivityStatusEnum activityStatusEnum = calculateActivityStatus(goods.getBeginTime(), goods.getEndTime());
            goods.setActivityStatus(activityStatusEnum.getStatus());
            return Response.success(goods);
        }

        // 查询秒杀活动
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        // 查询秒杀商品
        SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByPrimaryKey(goodsId);
        if (Objects.isNull(seckillGoodsDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        // 查询商品
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(seckillGoodsDO.getGoodsId());
        if (Objects.isNull(goodsDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        // 查询商品详情
        GoodsDetailDO goodsDetailDO = goodsDetailDOMapper.selectByPrimaryKey(goodsId);
        // 查询商品图片
        List<GoodsImgDO> goodsImageDOList = goodsImgDOMapper.selectByGoodsId(seckillGoodsDO.getGoodsId());
        List<String> urlList = null;
        if (!CollectionUtils.isEmpty(goodsImageDOList)) {
            urlList = goodsImageDOList.stream().map(GoodsImgDO::getImgUrl).toList();
        }
        // 计算活动状态
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO.getBeginTime(), activityDO.getEndTime());
        // 拼装
        FindSeckillGoodsDetailRspVO detailRspVO = new FindSeckillGoodsDetailRspVO();
        detailRspVO.setId(seckillGoodsDO.getId());
        detailRspVO.setGoodsId(goodsDO.getId());
        detailRspVO.setActivityId(seckillGoodsDO.getActivityId());
        detailRspVO.setGoodsImgs(urlList);
        detailRspVO.setSeckillPrice(seckillGoodsDO.getSeckillPrice());
        detailRspVO.setSeckillTotal(seckillGoodsDO.getSeckillTotal());
        detailRspVO.setSeckillStock(seckillGoodsDO.getSeckillStock());
        detailRspVO.setActivityStatus(activityStatusEnum.getStatus());
        detailRspVO.setBeginTime(activityDO.getBeginTime());
        detailRspVO.setEndTime(activityDO.getEndTime());

        if (Objects.nonNull(goodsDO)) {
            detailRspVO.setGoodsName(goodsDO.getGoodsName());
            detailRspVO.setGoodsPrice(goodsDO.getGoodsPrice());
        }

        if (Objects.nonNull(goodsDetailDO)) {
            detailRspVO.setGoodsDetail(goodsDetailDO.getDetailContent());
        }

        // 写入缓存
        log.info("==> 商品详情缓存未命中，将数据写入 Redis, redisKey: {}", key);
        stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonString(detailRspVO),
                RedisKeyConstants.GOODS_DETAIL_TTL_MINUTES, TimeUnit.MINUTES);

        return Response.success(detailRspVO);
    }

    /**
     * 手动预热秒杀商品缓存
     * @param activityId
     * @return
     */
    @Override
    public Response<?> preheatActivityGoods(Long activityId) {
        log.info("==> 手动预热秒杀商品缓存, activityId: {}", activityId);
        // 查询活动
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            log.info("==> 预热跳过：活动不存在, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        // 计算活动过期时间
        Long endTime = RedisKeyConstants.calculateTtlSeconds(activityDO.getEndTime());
        if (Objects.nonNull(endTime) && endTime < 0) {
            log.info("==> 预热跳过：活动已结束, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }

        // 查询活动下的商品
        List<SeckillGoodsDO> seckillGoodsDOS = seckillGoodsDOMapper.selectByActivityId(activityId);
        if (CollectionUtils.isEmpty(seckillGoodsDOS)) {
            log.info("==> 预热跳过：活动下无商品, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_GOODS_EMPTY);
        }
        // 批量查询商品原价
        List<Long> goodsIdList = seckillGoodsDOS.stream().map(SeckillGoodsDO::getGoodsId).toList();
        List<GoodsDO> goodsIds = goodsDOMapper.selectByIds(goodsIdList);
        Map<Long, GoodsDO> goodsMap = goodsIds.stream().collect(Collectors.toMap(GoodsDO::getId, goodsDO -> goodsDO));
        // 预热商品列表缓存
        String listKey = RedisKeyConstants.GOODS_LIST_PREFIX + activityId;
        List<FindSeckillGoodsListRspVO> listRspVOS = new ArrayList<>();
        for (SeckillGoodsDO sg : seckillGoodsDOS) {
            FindSeckillGoodsListRspVO vo = new FindSeckillGoodsListRspVO();
            vo.setId(sg.getId());
            vo.setGoodsId(sg.getGoodsId());
            vo.setActivityId(sg.getActivityId());
            vo.setSeckillTitle(sg.getSeckillTitle());
            vo.setSeckillImg(sg.getSeckillImg());
            vo.setSeckillPrice(sg.getSeckillPrice());
            vo.setSeckillTotal(sg.getSeckillTotal());
            vo.setSeckillStock(sg.getSeckillStock());
            vo.setActivityStatus(calculateActivityStatus(activityDO).getStatus());
            vo.setBeginTime(activityDO.getBeginTime());
            vo.setEndTime(activityDO.getEndTime());

            // 设置商品原价
            GoodsDO goodsDO = goodsMap.get(sg.getGoodsId());
            if (Objects.nonNull(goodsDO)) {
                vo.setGoodsPrice(goodsDO.getGoodsPrice());
            }

            listRspVOS.add(vo);
        }

        stringRedisTemplate.opsForValue().set(listKey, JsonUtils.toJsonString(listRspVOS),
                endTime, TimeUnit.SECONDS);
        log.info("==> 预热商品列表缓存成功, key: {}, TTL: {}s", listKey, endTime);
        // 缓存每个商品的详情
        for (SeckillGoodsDO sg : seckillGoodsDOS) {
            String detailKey = RedisKeyConstants.GOODS_DETAIL_PREFIX + activityId + ":" + sg.getGoodsId();

            // 查询商品基本信息
            GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(sg.getGoodsId());

            // 查询商品轮播图
            List<GoodsImgDO> goodsImgDOS = goodsImgDOMapper.selectByGoodsId(sg.getGoodsId());
            List<String> goodsImgs = null;
            if (CollUtil.isNotEmpty(goodsImgDOS)) {
                goodsImgs = goodsImgDOS.stream()
                        .map(GoodsImgDO::getImgUrl)
                        .toList();
            }

            // 查询商品详情 HTML
            GoodsDetailDO goodsDetailDO = goodsDetailDOMapper.selectByGoodsId(sg.getGoodsId());

            // 组装详情 VO
            FindSeckillGoodsDetailRspVO detailVO = FindSeckillGoodsDetailRspVO.builder()
                    .id(sg.getId())
                    .goodsId(sg.getGoodsId())
                    .activityId(sg.getActivityId())
                    .seckillPrice(sg.getSeckillPrice())
                    .seckillTotal(sg.getSeckillTotal())
                    .seckillStock(sg.getSeckillStock())
                    .activityStatus(calculateActivityStatus(activityDO).getStatus())
                    .beginTime(activityDO.getBeginTime())
                    .endTime(activityDO.getEndTime())
                    .goodsImgs(goodsImgs)
                    .build();

            // 设置商品名称和原价
            if (Objects.nonNull(goodsDO)) {
                detailVO.setGoodsName(goodsDO.getGoodsName());
                detailVO.setGoodsPrice(goodsDO.getGoodsPrice());
            }

            // 设置商品详情 HTML
            if (Objects.nonNull(goodsDetailDO)) {
                detailVO.setGoodsDetail(goodsDetailDO.getDetailContent());
            }

            stringRedisTemplate.opsForValue().set(detailKey, JsonUtils.toJsonString(detailVO),
                    endTime, TimeUnit.SECONDS);
        }

        log.info("==> 预热活动 {} 的 {} 个商品详情缓存完成", activityId, seckillGoodsDOS.size());

        return Response.success();
    }

    /**
     * 计算活动状态
     *
     * @param beginTime
     * @Param endTime
     * @return
     */
    private ActivityStatusEnum calculateActivityStatus(LocalDateTime beginTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(beginTime)) {
            return ActivityStatusEnum.NOT_STARTED;
        } else if (now.isAfter(endTime)) {
            return ActivityStatusEnum.ENDED;
        } else {
            return ActivityStatusEnum.ING;
        }
    }

    /**
     * 计算活动状态
     * @Param activityDO
     * @return
     */
    private ActivityStatusEnum calculateActivityStatus(SeckillActivityDO activityDO) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activityDO.getBeginTime())) {
            return ActivityStatusEnum.NOT_STARTED;
        } else if (now.isAfter(activityDO.getEndTime())) {
            return ActivityStatusEnum.ENDED;
        } else {
            return ActivityStatusEnum.ING;
        }
    }

}
