package cn.changzer.NIO;


import org.junit.Test;

import java.nio.ByteBuffer;

/***
 * Buffer测试类
 */
public class TestBuffer {

    @Test
    public void test1(){
        //1. 分配一个指定大小的缓冲区
        ByteBuffer buf = ByteBuffer.allocate(1024);
        System.out.println("-----------------allocate()----------------");
        System.out.println(buf.position());// 0: 表示当前的位置为0
        System.out.println(buf.limit());// 1024: 表示界限为1024，前1024个位置是允许我们读写的
        System.out.println(buf.capacity());//1024：表示容量大小为1024

        //2. 利用 put() 存入数据到缓冲区中
        System.out.println("-----------------put()----------------");
        String str = "itheima";
        buf.put(str.getBytes());
        System.out.println(buf.position());// 7表示下一个可以写入的位置是7,因为我们写入的字节是7个,从0开始已经写了7个，位置为8的position为7
        System.out.println(buf.limit());// 1024：表示界限为1024，前1024个位置是允许我们读写的
        System.out.println(buf.capacity());//1024：表示容量大小为1024

        //3. 切换读取数据模式
        System.out.println("-----------------flip()----------------");
        buf.flip();
        System.out.println(buf.position());// 0: 读取的起始位置为0
        System.out.println(buf.limit());// 7: 表示界限为7，前7个位置有数据可以读取
        System.out.println(buf.capacity());// 1024:表示容量大小为1024

        //4. 利用 get() 读取缓冲区中的数据
        System.out.println("-----------------get()----------------");
        byte[] dst = new byte[buf.limit()];//创建一个界限为limit大小的字节数组
        buf.get(dst);//批量将limit大小的字节写入到dst字节数组中
        System.out.println(new String(dst, 0, dst.length));//结果为itheima

        System.out.println(buf.position());//7: 读取的位置变为7,因为前面的7个字节数据已经全部读取出去,下一个可读取的位置为7，从0开始的
        System.out.println(buf.limit());//7: 可读取的界限大小为7
        System.out.println(buf.capacity());// 1024: 表示容量大小为1024

        //5. rewind() : 可重复读
        System.out.println("-----------------rewind()----------------");
        buf.rewind();// 将位置设为为 0,从头开始读取
        System.out.println(buf.position());// 0
        System.out.println(buf.limit());// 7
        System.out.println(buf.capacity());// 1024

        //6. clear() : 清空缓冲区. 但是缓冲区中的数据依然存在，但是处于“被遗忘”状态
        System.out.println("-----------------clear()----------------");
        buf.clear();
        System.out.println(buf.position());// 0
        System.out.println(buf.limit());// 1024
        System.out.println(buf.capacity());// 1024
        System.out.println((char)buf.get());//i

    }

    @Test
    public void test2(){
        String str = "itheima";

        ByteBuffer buf = ByteBuffer.allocate(1024);

        buf.put(str.getBytes());// 将str写入到buf缓冲区中

        buf.flip();//转换为读模式

        byte[] dst = new byte[buf.limit()];//定义一个字节数组
        buf.get(dst, 0, 2);//将前2个字节批量写入到dst字节数组中
        System.out.println(new String(dst, 0, 2));//打印结果为it
        System.out.println(buf.position());//当前下一个读取的位置为2

        //mark() : 标记
        buf.mark();

        buf.get(dst, 2, 2);//从第3个位置开始将2个字节批量写入到dst字节数组中
        System.out.println(new String(dst, 2, 2));//打印结果为he
        System.out.println(buf.position());// 当前下一个读取的位置为4

        //reset() : 恢复到 mark 的位置
        buf.reset();
        System.out.println(buf.position());// 2

        //判断缓冲区中是否还有剩余数据
        if(buf.hasRemaining()){
            //获取缓冲区中可以操作的数量
            System.out.println(buf.remaining());// 5: 返回 position 和 limit 之间的元素个数
        }
    }

    @Test
    public void test3(){
        //分配直接缓冲区
        ByteBuffer buf = ByteBuffer.allocateDirect(1024);
        System.out.println(buf.isDirect());
    }

}
