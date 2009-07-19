package org.fusesource.rmiviajms;

import junit.framework.TestCase;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.fusesource.rmiviajms.internal.JMSRemoteSystem;

import javax.jms.Destination;

/**
 * @author chirino
 */
public class JMSRemoteObjectTest extends TestCase {

    private BrokerService broker;

    public static interface IHelloWorld extends Remote {
        public String hello() throws RemoteException;
        public void world(IHelloWorldCallback callback) throws RemoteException;
    }

    public  static interface IHelloWorldCallback extends Remote {
        public void execute(String value) throws RemoteException;
    }

    public static class HelloWorld implements IHelloWorld {
        public String hello() {
            return "hello";
        }

        public void world(IHelloWorldCallback callback) throws RemoteException {
            callback.execute("world");
        }
    }

    public static class HelloWorldCallback extends JMSRemoteObject implements IHelloWorldCallback {
        String value;
        CountDownLatch latch = new CountDownLatch(1);

        HelloWorldCallback() throws RemoteException {
        }

        public HelloWorldCallback(Destination destination) throws RemoteException {
            super(destination);
        }

        public void execute(String value) {
            this.value=value;
            latch.countDown();
        }
    }

    @Override
    protected void setUp() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:61616");
        broker.setUseJmx(false);
        broker.start();
    }

    @Override
    protected void tearDown() throws Exception {
        JMSRemoteObject.resetSystem();
        broker.stop();
    }

    public void testHelloWorld() throws RemoteException {
        HelloWorld object = new HelloWorld();
        Remote proxy = JMSRemoteObject.exportObject(object);
        assertTrue(proxy instanceof IHelloWorld);

        IHelloWorld hwp = (IHelloWorld) proxy;
        assertEquals("hello", hwp.hello());
    }


    public void testHelloWorldCallback() throws RemoteException, InterruptedException {
        HelloWorld object = new HelloWorld();
        IHelloWorld proxy = (IHelloWorld)JMSRemoteObject.exportObject(object);

        HelloWorldCallback callback = new HelloWorldCallback();
        proxy.world(callback);

        assertTrue(callback.latch.await(5, TimeUnit.SECONDS));
        assertEquals("world", callback.value);

    }

    public void testHelloWorldAtKnownDestination() throws RemoteException {
        HelloWorld object = new HelloWorld();
        Remote proxy = JMSRemoteObject.exportObject(object, new ActiveMQQueue("FOO"));
        assertTrue(proxy instanceof IHelloWorld);

        IHelloWorld hwp = (IHelloWorld) proxy;
        assertEquals("hello", hwp.hello());
    }


    public void testHelloWorldCallbackAtKnownDestination() throws RemoteException, InterruptedException {
        HelloWorld object = new HelloWorld();
        IHelloWorld proxy = (IHelloWorld)JMSRemoteObject.exportObject(object);

        HelloWorldCallback callback = new HelloWorldCallback(new ActiveMQQueue("BAR"));
        proxy.world(callback);

        assertTrue(callback.latch.await(5, TimeUnit.SECONDS));
        assertEquals("world", callback.value);

    }

}