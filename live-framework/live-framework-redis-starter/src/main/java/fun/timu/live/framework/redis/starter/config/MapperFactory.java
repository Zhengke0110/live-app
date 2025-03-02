package fun.timu.live.framework.redis.starter.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.cache.support.NullValue;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class MapperFactory {

    public static ObjectMapper newInstance() {
        return initMapper(new ObjectMapper(), (String) null);
    }

    /**
     * 初始化ObjectMapper对象，并配置其序列化和反序列化行为
     *
     * @param mapper                待初始化的ObjectMapper对象
     * @param classPropertyTypeName 类属性类型名称，用于指定类型信息
     * @return 初始化后的ObjectMapper对象
     */
    private static ObjectMapper initMapper(ObjectMapper mapper, String classPropertyTypeName) {
        // 注册自定义序列化器，用于处理空值的序列化
        mapper.registerModule(new SimpleModule().addSerializer(new MapperNullValueSerializer(classPropertyTypeName)));

        // 根据类属性类型名称配置默认类型信息的序列化方式
        if (StringUtils.hasText(classPropertyTypeName)) {
            // 如果提供了类属性类型名称，则使用该名称作为类型信息
            mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, classPropertyTypeName);
        } else {
            // 如果未提供类属性类型名称，则使用属性方式序列化类型信息
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        }

        // 禁用反序列化时对未知属性的失败处理，提高灵活性和容错性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 返回初始化后的ObjectMapper对象
        return mapper;
    }


    private static class MapperNullValueSerializer extends StdSerializer<NullValue> {
        private static final long serialVersionUID = 1999052150548658808L;
        private final String classIdentifier;

        /**
         * @param classIdentifier can be {@literal null} and will
         *                        be defaulted to {@code @class}.
         */
        MapperNullValueSerializer(String classIdentifier) {
            super(NullValue.class);
            this.classIdentifier = StringUtils.hasText(classIdentifier) ? classIdentifier : "@class";
        }


        /**
         * 序列化NullValue对象为JSON格式
         * 此方法主要用于将NullValue对象转换成JSON格式的字符串并输出
         * 它在JSON序列化过程中被调用，用于处理NullValue类型的对象
         *
         * @param value    待序列化的NullValue对象，表示一个空值
         * @param jgen     用于生成JSON格式数据的工具对象，通过它来写入JSON数据
         * @param provider 提供序列化功能的工具对象，可用于获取序列化所需的配置或辅助对象
         * @throws IOException 如果在序列化过程中发生I/O错误，如无法写入输出流
         */
        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            // 开始一个新的JSON对象
            jgen.writeStartObject();
            // 在JSON对象中写入一个字符串字段，标识NullValue的类名
            jgen.writeStringField(classIdentifier, NullValue.class.getName());
            // 结束JSON对象
            jgen.writeEndObject();
        }
    }
}