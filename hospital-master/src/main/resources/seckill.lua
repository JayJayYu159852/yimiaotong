-- =====================================================
-- 专家号秒杀 Lua 脚本（原子操作）
-- =====================================================
-- 职责：判断库存 + 判断是否重复 + 扣库存 + 记录用户 + 发送Stream
-- KEYS[1] = stockKey    库存key    "seckill:stock:{planId}"
-- KEYS[2] = orderKey    预约记录key "seckill:order:{planId}"
-- ARGV[1] = planId      出诊编号
-- ARGV[2] = cardId      就诊卡号
-- ARGV[3] = orderId     全局唯一订单ID
-- ARGV[4] = accountId   账号编号
-- ARGV[5] = timePeriod  时间段
-- 返回值：0=成功, 1=库存不足, 2=重复预约
-- =====================================================

-- 1. 判断库存
local stock = redis.call('get', KEYS[1])
if (not stock or tonumber(stock) <= 0) then
    return 1
end

-- 2. 判断是否已预约（同一出诊同一就诊卡只能预约一次）
local exists = redis.call('sismember', KEYS[2], ARGV[2])
if (exists == 1) then
    return 2
end

-- 3. 扣库存
redis.call('incrby', KEYS[1], -1)

-- 4. 记录用户（防重复预约）
redis.call('sadd', KEYS[2], ARGV[2])

-- 5. 发送消息到 Redis Stream（异步下单）
redis.call('xadd', 'stream.appointment.orders', '*',
    'planId', ARGV[1],
    'cardId', ARGV[2],
    'orderId', ARGV[3],
    'accountId', ARGV[4],
    'timePeriod', ARGV[5]
)

return 0
