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
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(listRspVOS),
                RedisKeyConstants.GOODS_LIST_TTL_MINUTES, TimeUnit.MINUTES);

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
        return Response.success(detailRspVO);
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

}
