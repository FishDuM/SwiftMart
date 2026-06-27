package hk.ljx.swiftmart.order.controller;

import hk.ljx.swiftmart.common.aspect.ApiOperationLog;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.order.modal.vo.DoSeckillReqVO;
import hk.ljx.swiftmart.order.modal.vo.DoSeckillRspVO;
import hk.ljx.swiftmart.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 秒杀下单
     *
     * @param reqVO
     * @return
     */
    @PostMapping
    @ApiOperationLog(description = "秒杀下单")
    public Response<DoSeckillRspVO> doSeckill(@RequestBody @Validated DoSeckillReqVO reqVO) {
        return orderService.doSeckill(reqVO);
    }
}