package janw4ld.utils;

import java.util.AbstractMap;

public class Pair<F, S> extends AbstractMap.SimpleImmutableEntry<F, S> {
  @Override
  public String toString() {
    return "(" + getKey() + ", " + getValue() + ")";
  }

  public static <F, S> Pair<F, S> of(F first, S second) {
    return new Pair<>(first, second);
  }

  public static <F, S> Pair<F, S> of(F first) {
    return new Pair<>(first, null);
  }

  public F first() {
    return getKey();
  }

  public S second() {
    return getValue();
  }

  public Pair(F first, S second) {
    super(first, second);
  }
}
