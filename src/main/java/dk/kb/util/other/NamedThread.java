package dk.kb.util.other;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Autoclosable thread namer. To be used like
 * <code>
 *     try (NamedThread named = new NamedThread("threadname")) {
 *
 *     }
 * </code>
 *
 * <code>
 *     try (NamedThread named = NamedThread.as("threadname")) {
 *
 *     }
 * </code>
 *
 * It will rename the current thread while inside the try-block and restore it's name
 * upon closing.
 *
 * Most useful if you log the thread name as part of your log lines.
 *
 * Setting the thread name to some value means you do not have to log this value as part of every log statement
 */
public class NamedThread implements AutoCloseable {
    
    
    private final Thread currentThread;
    private final String oldName;
    private final String name;
    
    /**
     * Name the thread from the line number is was created on
     * This format is recognized by IntelliJ idea for quick access to the file
     */
    public NamedThread() {
        //[0] is right here
        //[1] is this method
        //[2] is the caller
        Thread currentThread = Thread.currentThread();
        StackTraceElement caller = currentThread.getStackTrace()[2];
        this.name = caller.getClassName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + ") ";
        this.currentThread = currentThread;
        oldName = this.currentThread.getName();
        this.currentThread.setName(name);
    }
    
    /**
     * Name the thread
     * @param name the name to set
     */
    public NamedThread(String name) {
        this.name = name;
        currentThread = Thread.currentThread();
        oldName = currentThread.getName();
        currentThread.setName(name);
    }
    
    /**
     * Set the name of the thread
     * @param name the new thread name
     * @return a NamedThread
     * @see #NamedThread(String)
     */
    public static NamedThread as(String name) {
        return new NamedThread(name);
    }
    
    /**
     * Append the name to the thread name, separated by ->
     * @param name the postfix to the threadname
     * @return a NamedThread
     */
    public static NamedThread postfix(String name) {
        String parentName = Thread.currentThread().getName();
        String threadName = parentName + "->" + name;
        return new NamedThread(threadName);
    }
    
    
    @Override
    public void close() {
        currentThread.setName(oldName);
    }
    
    public String postfixed(String newName) {
        return name + "->" + newName;
    }
    
    public String getName() {
        return Thread.currentThread().getName();
    }
    
    public String getOldName() {
        return oldName;
    }
    
    /**
     * Wrapper to a function to execute it in a named thread.
     *
     * To be used like
     *
     * <code>
     *     .map(NamedThread.namedThread(a -> doSomethingAdvanced(a),
     *                                 a -> a.toString()))
     * </code>
     *
     * Each invocation of doSomethingAdvanced will run with a thread named
     * from the input value a. This is especially relevant when working with parallel streams.
     *
     * @param function the function to wrap
     * @param namingFunction The function to compute a name for the invocation
     * @param <T> The input type to the function
     * @param <R> the return type of the function
     * @return a function that will name the thread before each invocation
     */
    public static <T, R> Function<T, R> namedThread(Function<T, R> function, Function<T, String> namingFunction) {
        return t -> {
            try (NamedThread namedThread = new NamedThread(namingFunction.apply(t))) {
                return function.apply(t);
            }
        };
    }
    
    /**
     * Same as #namedThread(Function, Function) but for a Consumer
     * @see #namedThread(Function, Function)
     * @param consumer the consumer to wrap
     * @param namingFunction The function to compute a name for the invocation
     * @param <T> the input type of the consumer
     * @return a wrapped consumer
     */
    public static <T> Consumer<T> namedThread(Consumer<T> consumer, Function<T, String> namingFunction) {
        return t -> {
            try (NamedThread namedThread = new NamedThread(namingFunction.apply(t))) {
                consumer.accept(t);
            }
        };
        
    }
    
    /**
     * Same as #namedThread(Function, Function) but for a Predicate
     * @see #namedThread(Function, Function)
     * @param predicate the predicate to wrap
     * @param namingFunction The function to compute a name for the invocation
     * @param <T> the input type of the predicate
     * @return a wrapped predicate
     */
    public static <T> Predicate<T> namedThread(Predicate<T> predicate, Function<T, String> namingFunction) {
        return t -> {
            try (NamedThread namedThread = new NamedThread(namingFunction.apply(t))) {
                return predicate.test(t);
            }
        };
        
    }
}
