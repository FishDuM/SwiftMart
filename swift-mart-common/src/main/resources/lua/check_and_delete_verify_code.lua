-- KEY[1]: redis 验证码的key
-- ARGV[1]: 验证码
local storedKey = redis.call('GET', KEYS[1])
if storedKey == false then
    return 0
end

if storedKey == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end

return 0