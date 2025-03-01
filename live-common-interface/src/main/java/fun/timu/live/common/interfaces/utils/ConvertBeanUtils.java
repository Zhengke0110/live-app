package fun.timu.live.common.interfaces.utils;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class ConvertBeanUtils {

    /**
     * 将一个对象转换为另一个指定类型的对象
     * <p>
     * 此方法用于在不同类之间进行属性复制，常用于数据传输对象（DTO）与实体对象（Entity）之间的转换
     * 它依赖于Apache Commons BeanUtils库中的copyProperties方法，该方法能够自动将源对象的属性值复制到目标对象中
     *
     * @param source      要转换的源对象，可以是任何类型的对象，但通常用于转换自定义类对象
     * @param targetClass 目标对象的类类型，使用泛型指定，以确保类型安全
     * @param <T>         泛型参数，表示目标对象的类型
     * @return 返回转换后的目标类型对象，如果源对象为null，则返回null
     * @throws IllegalArgumentException 如果目标Class类型为null，或者源对象与目标对象无法进行属性复制，将抛出此异常
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T t = newInstance(targetClass);
        BeanUtils.copyProperties(source, t);
        return t;
    }


    /**
     * 将源列表中的元素转换为目标类类型的列表
     *
     * @param <K>         源列表元素的类型
     * @param <T>         目标列表元素的类型
     * @param sourceList  源列表，其中元素将被转换
     * @param targetClass 目标类类型，源列表元素将被转换为该类型
     * @return 转换后的目标列表如果源列表为null，则返回null
     */
    public static <K, T> List<T> convertList(List<K> sourceList, Class<T> targetClass) {
        // 检查源列表是否为null，如果是，则直接返回null
        if (sourceList == null) {
            return null;
        }
        // 初始化目标列表，容量为源列表大小除以装载因子加上1，以减少重新哈希的次数
        List<T> targetList = new ArrayList<>((int) (sourceList.size() / 0.75) + 1);
        // 遍历源列表，将每个元素转换为目标类类型，并添加到目标列表中
        for (K source : sourceList) {
            targetList.add(convert(source, targetClass));
        }
        // 返回转换后的目标列表
        return targetList;
    }

    /**
     * 创建指定类的新实例。
     *
     * @param targetClass 目标类的 Class 对象
     * @param <T>         目标类的类型参数
     * @return 返回目标类的新实例
     * @throws BeanInstantiationException 如果实例化过程中发生错误，则抛出此异常
     */
    private static <T> T newInstance(Class<T> targetClass) {
        try {
            // 使用反射机制创建目标类的新实例
            return targetClass.newInstance();
        } catch (Exception e) {
            // 如果实例化过程中发生异常，抛出自定义的 BeanInstantiationException 异常
            throw new BeanInstantiationException(targetClass, "实例化错误", e);
        }
    }

}