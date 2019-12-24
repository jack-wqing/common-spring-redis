说明：基于Spring cache模块：写注解版缓存组件
           本地和远程缓存：都对缓存击穿、穿透雪崩进行了处理
      1、支持远程共享缓存：使用redis
      2、支持本地缓存：使用Caffeine
   功能：1、redis：支持在方法级别上，自定义过期时间
                          需要体统提供：RedisConnectionFactory Bean实现
       2、Caffeine：支持在方法级别上配置：过期时间，初始化容量，最大容量(采用expireAfterWrite过期方式)
 
  使用方式：
     1、引入本组件依赖
     redis注解说明：
          @Cacheable(cacheManager = "redisCache", value = "findById#e10")
                 说明：cacheManager = "redisCache"：指明缓存管理必须是redisCache字符串
              value = "findById#e10"：findById#：固定格式，
                         e10：e为开头，跟着数字为过期时间
     2、本地缓存说明：
         1、不配置缓存管理器：默认为本地缓存
            @Cacheable(value = "findById#e1#i200#m2000")
         2、或者配置管理器
            @Cacheable(cacheManager = "localCache", value = "findById#e1#i200#m2000")
         
      
      value = "findById#e1#i200#m2000":findById#:为固定格式
                                              e1：e为开头，跟着数字为过期时间
                                              i200：i为开头，跟着数字，表示初始本地容量
                                              m2000：m为开通，跟着数组，表示最大容量
                                              