package com.boringdroid.systemui;

public class ReflectUtils {

    /**
     * 获取参数类型的类数组。
     *
     * @param args 参数对象数组。
     * @return 参数类型的类数组。
     */
    private static Class<?>[] getParameterTypes(Object... args) {
        if (args == null || args.length == 0) {
            return new Class[0];
        }

        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            // 如果参数为 null，则使用 Object.class 作为默认类型
            parameterTypes[i] = (args[i] != null) ? args[i].getClass() : Object.class;
        }
        return parameterTypes;
    }

    /**
     * 通过反射调用静态或实例方法并返回结果对象。
     *
     * @param clazz      目标类。
     * @param obj        目标对象（如果是静态方法可以传入 null）。
     * @param methodName 方法名。
     * @param returnType 返回值类型。
     * @param args       方法参数。
     * @param <T>        返回值的泛型类型。
     * @return 方法调用的结果。
     * @throws Exception 如果反射调用失败。
     */
    public static <T> T invokeObject(Class<?> clazz, Object obj, String methodName, Class<T> returnType, Object... args) throws Exception {
        // 获取方法参数类型
        Class<?>[] parameterTypes = getParameterTypes(args);

        // 获取方法对象并设置可访问
        java.lang.reflect.Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        // 调用方法并返回结果
        return returnType.cast(method.invoke(obj, args));
    }

    /**
     * 通过反射调用静态或实例方法，不返回结果。
     *
     * @param clazz         目标类。
     * @param obj           目标对象（如果是静态方法可以传入 null）。
     * @param methodName    方法名。
     * @param parameterTypes 方法参数类型。
     * @param args          方法参数。
     * @throws Exception 如果反射调用失败。
     */
    public static void invokeVoidMethod(Class<?> clazz, Object obj, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        java.lang.reflect.Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(obj, args);
    }
}
