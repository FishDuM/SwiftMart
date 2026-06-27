package hk.ljx.swiftmart.goods.service.impl;

import cn.hutool.core.collection.CollUtil;
import hk.ljx.swiftmart.common.domain.dataobject.*;
import hk.ljx.swiftmart.common.domain.mapper.*;
import hk.ljx.swiftmart.common.enums.ActivityStatusEnum;
import hk.ljx.swiftmart.common.enums.ResponseCodeEnum;
import hk.ljx.swiftmart.common.exception.BizException;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsDetailReqVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsDetailRspVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListReqVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListRspVO;
import hk.ljx.swiftmart.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
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
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO);
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
        return Response.success(listRspVOS);
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
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO);
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
     * @param activityDO
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
