package free.servpp.sppframe.impls;

import free.servpp.sppframe.IAtomicService;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public class ServiceContainerTool {
    private static boolean hasInterface(Class<?> clazz, Class<?> targetInterface) {
        // 获取类实现的所有接口
        Class<?>[] interfaces = clazz.getInterfaces();

        // 遍历检查是否实现了目标接口
        for (Class<?> i : interfaces) {
            if (i.equals(targetInterface) || hasInterface(i, targetInterface)) {
                return true;
            }
        }

        // 如果类有父类，则继续检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return hasInterface(superClass, targetInterface);
        }

        return false;
    }

    public static String getServiceName(Class<?> clazz){
        Class[] classes = clazz.getInterfaces();
        for (Class c:classes){
            if(hasInterface(c, IAtomicService.class)){
                return c.getSimpleName();
            }
        }
        return null;
    }
}
