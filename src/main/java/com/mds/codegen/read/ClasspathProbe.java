package com.mds.codegen.read;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 判断"某个类在不在被读工程的 classpath 上"。
 *
 * <p>用途：配置里写了枚举全限定名，但那个枚举还不存在时，生成器要能发现并补一个骨架。
 * 这里只做 {@code Class.forName(name, false, loader)}，**不初始化**类，
 * 所以不会触发静态块。
 */
public final class ClasspathProbe {

    private ClasspathProbe() {
    }

    /** 建一个隔离的 loader（父加载器用平台加载器，避免插件自身的依赖干扰）。 */
    public static URLClassLoader loader(ReadContext ctx) {
        List<URL> urls = new ArrayList<>();
        for (Path p : ctx.classpath()) {
            try {
                urls.add(p.toUri().toURL());
            } catch (IOException e) {
                throw new ReaderException("classpath 项无法转 URL: " + p, e);
            }
        }
        return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    /** 类是否存在（不初始化）。 */
    public static boolean exists(ReadContext ctx, String fqn) {
        try (URLClassLoader loader = loader(ctx)) {
            Class.forName(fqn, false, loader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (NoClassDefFoundError e) {
            // 类文件在，但它依赖的某个类不在——当作"不存在"，让调用方生成骨架时能看见真实原因
            return false;
        }
    }
}
