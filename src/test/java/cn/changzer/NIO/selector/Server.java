package cn.changzer.NIO.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {
    public static void main(String[] args) {
        try {
            //1.获取管道
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            //2.设置非阻塞模式
            serverSocketChannel.configureBlocking(false);
            //3.绑定端口
            serverSocketChannel.bind(new InetSocketAddress(8888));
            //4.获取选择器
            Selector selector = Selector.open();
            //5.将通道注册到选择器上，并且开始指定监听的接收事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            //6.轮询已经就绪的事件
            while (selector.select() > 0){
                System.out.println("开启事件处理");
                //7.获取选择器中所有注册的通道中已准备好的事件
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                //8.开始遍历事件
                while (it.hasNext()){
                    SelectionKey selectionKey = it.next();
                    System.out.println("--->"+selectionKey);
                    //9.判断这个事件具体是啥
                    if (selectionKey.isAcceptable()){
                        //10.获取当前接入事件的客户端通道
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        //11.切换成非阻塞模式
                        socketChannel.configureBlocking(false);
                        //12.将本客户端注册到选择器
                        socketChannel.register(selector,SelectionKey.OP_READ);
                    }else if (selectionKey.isReadable()){
                        //13.获取当前选择器上的读
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        //14.读取
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int len;
                        while ((len = socketChannel.read(buffer)) > 0){
                            buffer.flip();
                            System.out.println(new String(buffer.array(),0,len));
                            //清除之前的数据（覆盖写入）
                            buffer.clear();
                        }
                    }
                    //15.处理完毕后，移除当前事件
                    it.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
