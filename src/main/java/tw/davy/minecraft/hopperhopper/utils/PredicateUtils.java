package tw.davy.minecraft.hopperhopper.utils;

import java.util.function.Predicate;

/**
 * @author Davy
 */
public class PredicateUtils {
    public static<T> Predicate<T> not(Predicate<T> p) {
        return p.negate();
    }
}
