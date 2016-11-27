package util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 获得{@link sun.misc.Unsafe}.
 *
 * @author skywalker
 */
public class UnsafeAccess {

    public static Unsafe UNSAFE;

    private UnsafeAccess() {}

    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
