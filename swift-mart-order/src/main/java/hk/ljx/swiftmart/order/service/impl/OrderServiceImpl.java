package hk.ljx.swiftmart.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import hk.ljx.swiftmart.common.domain.dataobject.GoodsDO;
import hk.ljx.swiftmart.common.domain.dataobject.SeckillActivityDO;
import hk.ljx.swiftmart.common.domain.dataobject.SeckillGoodsDO;
import hk.ljx.swiftmart.common.domain.dataobject.SeckillOrderDO;
import hk.ljx.swiftmart.common.domain.mapper.GoodsDOMapper;
import hk.ljx.swiftmart.common.domain.mapper.SeckillActivityDOMapper;
import hk.ljx.swiftmart.common.domain.mapper.SeckillGoodsDOMapper;
import hk.ljx.swiftmart.common.domain.mapper.SeckillOrderDOMapper;
import hk.ljx.swiftmart.common.enums.ResponseCodeEnum;
import hk.ljx.swiftmart.common.exception.BizException;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.order.enums.OrderStatusEnum;
import hk.ljx.swiftmart.order.modal.vo.DoSeckillReqVO;
import hk.ljx.swiftmart.order.modal.vo.DoSeckillRspVO;
import hk.ljx.swiftmart.order.service.OrderService;
import hk.ljx.swiftmart.order.utils.OrderLockUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private SeckillOrderDOMapper seckillOrderDOMapper;

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private OrderLockUtils orderLockUtils;

    /**
     * 秒杀下单
     *
     * @param reqVO
     * @return
     */
    @Override
    public Response<DoSeckillRspVO> doSeckill(DoSeckillReqVO reqVO) {
        Long activityId = reqVO.getActivityId();
        Long goodsId = reqVO.getGoodsId();

        // 校验是否登录
        long loginId = StpUtil.getLoginIdAsLong();
        log.info("当前用户ID:{}，正在秒杀", loginId);

        String key = loginId + ":" + activityId + ":" + goodsId;
        if (!orderLockUtils.tryLock(key)) {
            log.warn("==> 应用层锁拦截重复下单, userId: {}, activityId: {}, goodsId: {}", loginId, activityId, goodsId);
            throw new BizException(ResponseCodeEnum.SECKILL_ORDER_PROCESSING);
        }
        try {
            return processSeckill(activityId, goodsId, loginId);
        } finally {
            orderLockUtils.unlock(key);
        }
    }

    /**
     * 秒杀下单核心逻辑
     */
    private Response<DoSeckillRspVO> processSeckill(Long activityId, Long goodsId, long loginId) {
        // 校验秒杀活动
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        LocalDateTime beginTime = activityDO.getBeginTime();
        LocalDateTime endTime = activityDO.getEndTime();
        if (LocalDateTime.now().isBefore(beginTime)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);
        }
        if (LocalDateTime.now().isAfter(endTime)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }
        // 校验商品
        SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(activityId, goodsId);
        if (Objects.isNull(seckillGoodsDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        if (seckillGoodsDO.getSeckillStock() <= 0) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_SOLD_OUT);
        }

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);
        String orderNo = IdUtil.getSnowflakeNextIdStr();
        SeckillOrderDO orderDO = transactionTemplate.execute(status -> {
            // 扣减库存
            int result = seckillGoodsDOMapper.deductStock(goodsId);
            if (result <= 0) {
                throw new BizException(ResponseCodeEnum.SECKILL_GOODS_SOLD_OUT);
            }
            // 生成订单
            SeckillOrderDO order = SeckillOrderDO.builder()
                    .activityId(activityId)
                    .goodsId(goodsId)
                    .userId(loginId)
                    .goodsImg(goodsDO.getGoodsImg())
                    .seckillPrice(seckillGoodsDO.getSeckillPrice())
                    .goodsName(goodsDO.getGoodsName())
                    .orderNo(orderNo)
                    .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                    .expireTime(expireTime)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(0)
                    .build();
            try {
                seckillOrderDOMapper.insert(order);
            } catch (DuplicateKeyException e) {
                log.warn("==> 重复下单, userId: {}, activityId: {}, goodsId: {}", loginId, activityId, goodsId);
                throw new BizException(ResponseCodeEnum.SECKILL_ORDER_DUPLICATE);
            }
            return order;
        });

        log.info("==> 秒杀下单成功, orderId: {}, orderNo: {}", orderDO.getId(), orderNo);

        // 组装响应数据
        DoSeckillRspVO rspVO = DoSeckillRspVO.builder()
                .orderId(orderDO.getId())
                .orderNo(orderNo)
                .goodsName(goodsDO.getGoodsName())
                .goodsImg(goodsDO.getGoodsImg())
                .seckillPrice(seckillGoodsDO.getSeckillPrice())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .expireTime(expireTime)
                .build();
        return Response.success(rspVO);
    }
}
