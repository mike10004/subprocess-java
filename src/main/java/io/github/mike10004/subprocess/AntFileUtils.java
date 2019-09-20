package io.github.mike10004.subprocess;

/**
 * @author Apache Ant authors https://ant.apache.org/
 */
class AntFileUtils {

    private AntFileUtils() {}

    /**
     * Close an {@link AutoCloseable} without throwing any exception
     * if something went wrong.  Do not attempt to close it if the
     * argument is null.
     *
     * @param ac AutoCloseable, can be null.
     * @since Ant 1.10.0
     */
    public static void close(AutoCloseable ac) {
        if (null != ac) {
            try {
                ac.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
