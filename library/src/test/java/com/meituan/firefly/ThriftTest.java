package com.meituan.firefly;

import com.meituan.firefly.testfirefly.TestService;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by ponyets on 15/6/23.
 */
public class ThriftTest {
    Thrift thrift = new Thrift();

    @Test
    public void shouldCreateService() {
        TestService service = thrift.create(TestService.class, new Thrift.SimpleTProtocolFactory() {
            @Override
            public TProtocol get() {
                return new TBinaryProtocol(new TMemoryInputTransport(new byte[]{}));
            }
        });
        assertThat(service).isNotNull();
    }

    @Test
    public void testInterceptorOrder() throws Exception {
        AtomicInteger ai = new AtomicInteger();
        OrderCheckInterceptor interceptor1 = new OrderCheckInterceptor(ai);
        OrderCheckInterceptor interceptor2 = new OrderCheckInterceptor(ai);
        TTransport transport = new FlushableMemoryBuffer(4096);
        com.meituan.firefly.testfirefly.TestService testService = thrift.create(com.meituan.firefly.testfirefly.TestService.class, new Thrift.SimpleTProtocolFactory() {
            @Override
            public TProtocol get() {
                return new TBinaryProtocol(transport);
            }
        }, interceptor1, interceptor2);
        NotifyCheck notifyCheck = new NotifyCheck(100);
        com.meituan.firefly.testthrift.TestService.Processor<com.meituan.firefly.testthrift.TestService.Iface> processor = new com.meituan.firefly.testthrift.TestService.Processor<>(notifyCheck);
        testService.notify(100);
        Assertions.assertThat(new int[]{interceptor2.inorder, interceptor1.inorder, interceptor1.outorder, interceptor2.outorder}).isEqualTo(new int[]{0, 1, 2, 3});
    }

    @Test
    public void testInterceptorAbort() throws Exception {
        TTransport transport = new FlushableMemoryBuffer(4096);
        com.meituan.firefly.testfirefly.TestService testService = thrift.create(com.meituan.firefly.testfirefly.TestService.class, new Thrift.SimpleTProtocolFactory() {
            @Override
            public TProtocol get() {
                return new TBinaryProtocol(transport);
            }
        }, new Interceptor() {
            @Override
            public Object intercept(Method method, Object[] args, TProtocol protocol, int seqId, Processor processor) throws Throwable {
                return null;
            }
        });
        NotifyCheck notifyCheck = new NotifyCheck(100);
        com.meituan.firefly.testthrift.TestService.Processor<com.meituan.firefly.testthrift.TestService.Iface> processor = new com.meituan.firefly.testthrift.TestService.Processor<>(notifyCheck);
        testService.notify(100);
        Assertions.assertThat(notifyCheck.notified).isFalse();
    }

    private static class OrderCheckInterceptor implements Interceptor {
        private final AtomicInteger ai;
        public int inorder;
        public int outorder;

        public OrderCheckInterceptor(AtomicInteger ai) {
            this.ai = ai;
        }

        @Override
        public Object intercept(Method method, Object[] args, TProtocol protocol, int seqId, Processor processor) throws Throwable {
            inorder = ai.getAndAdd(1);
            Object object = processor.process(method, args, protocol, seqId);
            outorder = ai.getAndAdd(1);
            return object;
        }
    }
}