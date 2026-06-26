-- 校验失败次数
local currentCount = tonumber(redis.call('GET', KEYS[1]) or '0')
if currentCount > tonumber(ARGV[1]) then
    return -1
end
-- 累加失败次数
local newCount = redis.call('INCR', KEYS[1])
-- 首次失败则设置过期时间
if newCount == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end

return newCount
