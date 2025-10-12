package com.hao.datacollector.study.jvm.classLoader;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * ClassLoaderTest
 * 实验目的：
 * 1. 观察自定义 ClassLoader 在父委派机制下加载类的顺序
 * 2. 验证核心类始终由 Bootstrap ClassLoader 加载
 * 3. 验证普通类最终由自定义 ClassLoader 加载
 */
@Slf4j
public class ClassLoaderTest {

    public static void main(String[] args) throws Exception {
        // 自定义 ClassLoader，父加载器是系统 ClassLoader（Application ClassLoader）
        MyClassLoader myLoader = new MyClassLoader(ClassLoader.getSystemClassLoader());

        testLoadNormalClass(myLoader);
        testLoadCoreClass(myLoader);

        // 再次加载普通类，验证父加载器缓存机制
        log.info("=== 再次加载普通类 ===");
        testLoadNormalClass(myLoader);
    }

    /**
     * 实验目的：观察自定义 ClassLoader 加载普通类的过程
     */
    static void testLoadNormalClass(MyClassLoader myLoader) {
        log.info("=== 加载普通类 ===");
        try {
            Class<?> myClass = myLoader.loadClass("com.hao.datacollector.study.jvm.classLoader.MyTestClass");
            log.info("MyTestClass 的 ClassLoader: {}", myClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            log.error("加载普通类失败", e);
        }
    }

    /**
     * 实验目的：验证核心类始终由 Bootstrap ClassLoader 加载
     */
    static void testLoadCoreClass(MyClassLoader myLoader) {
        log.info("=== 加载核心类 java.lang.String ===");
        try {
            Class<?> stringClass = myLoader.loadClass("java.lang.String");
            log.info("String 的 ClassLoader: {}", stringClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            log.error("加载核心类失败", e);
        }
    }
}

/**
 * 自定义 ClassLoader
 * 实验目的：
 * 1. 实现父委派加载失败后自己加载类
 * 2. 通过日志观察加载顺序
 */
@Slf4j
class MyClassLoader extends ClassLoader {

    public MyClassLoader(ClassLoader parent) {
        super(parent); // 指定父加载器
    }

    /**
     * 重写 loadClass 方法，打印每一级加载尝试
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        log.info("{} 尝试加载类: {}", this.getClass().getSimpleName(), name);

        // 先尝试父加载器
        ClassLoader parent = getParent();
        if (parent != null) {
            try {
                Class<?> parentClass = parent.loadClass(name);
                log.info("{} 父加载器 {} 成功加载类: {}", this.getClass().getSimpleName(),
                        parent.getClass().getSimpleName(), name);
                if (resolve) {
                    resolveClass(parentClass);
                }
                return parentClass;
            } catch (ClassNotFoundException e) {
                log.info("{} 父加载器 {} 未找到类: {}, 尝试自己加载", this.getClass().getSimpleName(),
                        parent.getClass().getSimpleName(), name);
            }
        }

        // 父加载器找不到，自己加载
        Class<?> clazz = findClass(name);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    /**
     * 自定义 findClass 实现
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        log.info("{} 自己尝试加载: {}", this.getClass().getSimpleName(), name);

        // 普通类 MyTestClass
        if (name.equals("com.hao.datacollector.study.jvm.classLoader.MyTestClass")) {
            try (InputStream is = getClass().getResourceAsStream("/com/hao/datacollector/study/jvm/classLoader/MyTestClass.class")) {
                if (is == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = is.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        throw new ClassNotFoundException(name);
    }
}
