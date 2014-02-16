package examples.example5.a;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Loading class added in Guava 14");
        Class<?> classIn14 = Class.forName("com.google.common.collect.ImmutableRangeMap");
        System.out.println("Success: " + classIn14.getName());

        System.out.println("Loading class added in Guava 15");
        Class<?> classIn15 = Class.forName("com.google.common.base.StandardSystemProperty");
        System.out.println("Success: " + classIn15.getName());

        System.out.println("Loading class added in Guava 16");
        Class<?> classIn16 = Class.forName("com.google.common.base.Converter");
        System.out.println("Success: " + classIn16.getName());
    }
}
