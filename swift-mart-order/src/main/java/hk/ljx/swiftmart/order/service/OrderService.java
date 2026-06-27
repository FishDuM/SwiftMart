package hk.ljx.swiftmart.order.service;

import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.order.modal.vo.DoSeckillReqVO;
import hk.ljx.swiftmart.order.modal.vo.DoSeckillRspVO;

public interface OrderService {

    /**
     * 秒杀下单
     *
     * @param reqVO
     * @return
     */
    Response<DoSeckillRspVO> doSeckill(DoSeckillReqVO reqVO);
}