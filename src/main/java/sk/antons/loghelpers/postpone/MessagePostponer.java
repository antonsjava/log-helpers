/*
 *
 */
package sk.antons.loghelpers.postpone;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 *
 * @author antons
 */
public abstract class MessagePostponer {

    protected BooleanSupplier enabled;
    protected Predicate<String> firstMessageWhen;
    protected Predicate<String> lastMessageWhen;
    protected Predicate<List<String>> processBeforeFirstWhen;
    protected Predicate<String> processBeforeFirstWhenAny;
    protected Predicate<List<String>> processAfterLastWhen;
    protected Predicate<String> processAfterLastWhenAny;
    protected Consumer<List<String>> beforeFirstAllMessageConsumer;
    protected Consumer<String> beforeFirstMessageConsumer;
    protected Consumer<String> afterLastMessageConsumer;
    protected Consumer<List<String>> afterLastallMessageConsumer;


    public boolean enabled() { return (enabled == null) || enabled.getAsBoolean(); }
    public MessagePostponer enabled(BooleanSupplier value) { this.enabled = value; return this; }
    public MessagePostponer firstMessageWhen(Predicate<String> value) { this.firstMessageWhen = value; return this; }
    public MessagePostponer lastMessageWhen(Predicate<String> value) { this.lastMessageWhen = value; return this; }
    public MessagePostponer processBeforeFirstWhen(Predicate<List<String>> value) { this.processBeforeFirstWhen = value; return this; }
    public MessagePostponer processBeforeFirstWhenAny(Predicate<String> value) { this.processBeforeFirstWhenAny = value; return this; }
    public MessagePostponer processAfterLastWhen(Predicate<List<String>> value) { this.processAfterLastWhen = value; return this; }
    public MessagePostponer processAfterLastWhenAny(Predicate<String> value) { this.processAfterLastWhenAny = value; return this; }
    public MessagePostponer beforeFirstAllMessageConsumer(Consumer<List<String>> value) { this.beforeFirstAllMessageConsumer = value; return this; }
    public MessagePostponer beforeFirstMessageConsumer(Consumer<String> value) { this.beforeFirstMessageConsumer = value; return this; }
    public MessagePostponer afterLastMessageConsumer(Consumer<String> value) { this.afterLastMessageConsumer = value; return this; }
    public MessagePostponer afterLastallMessageConsumer(Consumer<List<String>> value) { this.afterLastallMessageConsumer = value; return this; }


    protected abstract List<String> store();
    public List<String> currentlyStored() { return new ArrayList<>(store()); }

    public void beforeFirstMessage() {
        List<String> messages = store();
        if(messages.isEmpty()) return;
        boolean process = ((processBeforeFirstWhen != null) && processBeforeFirstWhen.test(messages));
        if(!process) {
            if(processBeforeFirstWhenAny != null) {
                for(String message : messages) {
                    process = processBeforeFirstWhenAny.test(message);
                    if(process) break;
                }
            }
        }
        if(process) {
            if(beforeFirstAllMessageConsumer != null) beforeFirstAllMessageConsumer.accept(messages);
            if(beforeFirstMessageConsumer != null) messages.stream().forEach(beforeFirstMessageConsumer);
        }
        messages.clear();
    }

    public void afterLastMessage() {
        List<String> messages = store();
        if(messages.isEmpty()) return;
        boolean process = ((processAfterLastWhen != null) && processAfterLastWhen.test(messages));
        if(!process) {
            if(processAfterLastWhenAny != null) {
                for(String message : messages) {
                    process = processAfterLastWhenAny.test(message);
                    if(process) break;
                }
            }
        }
        if(process) {
            if(afterLastallMessageConsumer != null) afterLastallMessageConsumer.accept(messages);
            if(afterLastMessageConsumer != null) messages.stream().forEach(afterLastMessageConsumer);
        }
        messages.clear();
    }

    public void addMessage(String message) {
        if(enabled()) {
            List<String> messages = store();
            if((firstMessageWhen != null) && firstMessageWhen.test(message)) {
                beforeFirstMessage();
                messages.add(message);
            } else if((lastMessageWhen != null) && lastMessageWhen.test(message)) {
                messages.add(message);
                afterLastMessage();
            } else {
                messages.add(message);
            }
        }
    }

    public static MessagePostponer simple() { return SimpleMessagePostponer.instance(); }
    public static MessagePostponer threadlocal() { return ThreadlocalMessagePostponer.instance(); }

    private static class SimpleMessagePostponer extends MessagePostponer {
        protected List<String> store = new ArrayList<>();
        public static SimpleMessagePostponer instance() { return new SimpleMessagePostponer(); }
        @Override protected List<String> store() { return store; }
    }
    private static class ThreadlocalMessagePostponer extends MessagePostponer {
        protected ThreadLocal<List<String>> store = ThreadLocal.withInitial(() -> new ArrayList<>());
        public static ThreadlocalMessagePostponer instance() { return new ThreadlocalMessagePostponer(); }
        @Override protected List<String> store() { return store.get(); }
    }
}
