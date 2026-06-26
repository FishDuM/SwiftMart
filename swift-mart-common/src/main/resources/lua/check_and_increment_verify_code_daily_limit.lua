-- 先判断有无超限
local num = tonumber(redis.call('GET', KEYS[1]) or '0')
if num >= tonumber(ARGV[1]) then
    return -1
end
-- 没有超限，则自增
local newNum = redis.call('INCR', KEYS[1])
-- 如果是第一次自增，则设置过期时间
if newNum == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return newNum