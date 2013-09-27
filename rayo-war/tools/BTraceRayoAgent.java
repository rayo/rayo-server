import com.sun.btrace.annotations.*;
import static com.sun.btrace.BTraceUtils.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@BTrace
public class BTraceRayoAgent {

        private static Map<String, AtomicInteger> histo = Collections.newHashMap();

        @OnMethod(
                clazz="/javax\\.media\\..*/",
                method="/.*/"
        )
        public static void m(@ProbeClassName String probeClass, @ProbeMethodName String probeMethod) {

                String key = strcat(probeClass, strcat(".",probeMethod));
                AtomicInteger ai = Collections.get(histo, key);
                if (ai == null) {
                        ai = Atomic.newAtomicInteger(1);
                        Collections.put(histo, key, ai);
                } else {
                        Atomic.incrementAndGet(ai);
                }
        }
        
        @OnMethod(
                clazz="/com\\.voxeo\\.moho\\..*/",
                method="/.*/"
        )
        public static void moho(@ProbeClassName String probeClass, @ProbeMethodName String probeMethod) {

                String key = strcat(probeClass, strcat(".",probeMethod));
                AtomicInteger ai = Collections.get(histo, key);
                if (ai == null) {
                        ai = Atomic.newAtomicInteger(1);
                        Collections.put(histo, key, ai);
                } else {
                        Atomic.incrementAndGet(ai);
                }
        }

        @OnTimer(4000)
        public static void print() {

                if (Collections.size(histo) != 0) {
                        printNumberMap("Component Histogram", histo);
                }
        }
}
