package Test;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.LinkedBlockingQueue;

import static org.testng.Assert.assertEquals;

@ApplicationScoped
class Stuff {
    private static final String TEST_CHANNEL_NAME = "TestChannel1";
    final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    void startup(@Observes StartupEvent event){
        System.out.println("Injected the listener");
    }

    @Inject
    @Channel(TEST_CHANNEL_NAME+"-out")
    Emitter<String> emitter;

    @Incoming(TEST_CHANNEL_NAME)
    public void listen(String message) throws InterruptedException {
        System.out.println(message);
        queue.put(message);
    }

    public void send(String message) {
        emitter.send(message);
    }

    public String take() throws InterruptedException {
        return queue.take();
    }

}

@QuarkusTest
public class BufferTests {

    final Stuff stuff;

    @Inject
    public BufferTests(Stuff stuff) {
        this.stuff = stuff;
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "b", "c", "d", "e"})
    @Timeout(10)
    public void ProducesTheBug(String testId) {
        final int TEST_SIZE = 1000;
        final var mainThread = Thread.currentThread();

        //Send
        new Thread(() -> {
            final var start = System.currentTimeMillis();

            for (int i = 0; i < TEST_SIZE; i++) {
                stuff.send(i+testId);
            }

            try {
                synchronized (this) {
                    wait(System.currentTimeMillis() - start + 9000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mainThread.interrupt();
        }).start();

        int i = 0;

        //Receive
        try {
            for (; i < TEST_SIZE; i++) {
                String item = stuff.take();
                assertEquals(testId, item.substring(item.length()-1));
            }
        } catch (InterruptedException ignored) {
        }
        assertEquals(i, TEST_SIZE);
    }
}
