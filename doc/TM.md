# 1. 从最简单的 TM 开始

**TM 通过维护 XID 文件来维护事务的状态，并提供接口供其他模块来查询某个事务的状态。**



### XID文件

  在MYDB中，每一个事务都有XID，XID唯一标识了这个事务。


事务的XID是从1开始标号，并自增，不可重复。特殊XID 0 为超级事务。当一些操作想在没申请事务的情况下进行，可以将操作的 XID 设置为 0。XID为 0 的事务的状态永远是 committed。

TransactionManager 维护了一个 XID 格式的文件，用来记录各个事务的状态。MYDB 中，每个事务都有下面的三种状态：

  1、active，正在运行，尚未结束

  2、commited，已提交

  3、aborted，已撤销（回滚）



-----
XID 文件给每个事务分配了一个字节的空间，用来保存其状态。同时，在 XID 文件的头部，还保存了一个 8 字节的数字，记录了这个 XID 文件管理的事务的个数。于是，事务 xid 在文件中的状态就存储在 (xid-1)+8 字节处，xid-1 是因为 xid 0（Super XID） 的状态不需要记录。



XID文件内容如下：

![image_1.8eee7392](http://images.changzer.cn/image_1.8eee7392.png)



定义 TransactionManager 接口供其他模块调用，用来创建事务和查询事务状态。

```java
 public interface TransactionManager { 
 long begin(); // 开启一个新事务 
 void commit(long xid); // 提交一个事务 
 void abort(long xid); // 取消一个事务 
 boolean isActive(long xid); // 查询一个事务的状态是否是正在进行的状态 
 boolean isCommitted(long xid); // 查询一个事务的状态是否是已提交 
 boolean isAborted(long xid); // 查询一个事务的状态是否是已取消 
 void close(); // 关闭TM 
}
```

### 实现

首先定义一些必要的常量和成员变量：

文件读写都采用了 NIO 方式的 FileChannel。

```java
public class TransactionManagerImpl implements TransactionManager{

// XID文件头长度
static final int LEN_XID_HEADER_LENGTH = 8;
// 每个事务的占用长度
private static final int XID_FIELD_SIZE = 1;
// 事务的三种状态
private static final byte FIELD_TRAN_ACTIVE = 0;
private static final byte FIELD_TRAN_COMMITTED = 1;
private static final byte FIELD_TRAN_ABORTED = 2;
// 超级事务，永远为commited状态
public static final long SUPER_XID = 0;
// XID 文件后缀
static final String XID_SUFFIX = ".xid";

private RandomAccessFile file;
private FileChannel fc;
private long xidCounter;
private Lock counterLock;

TransactionManagerImpl(RandomAccessFile raf, FileChannel fc) {
this.file = raf;
this.fc = fc;
counterLock = new ReentrantLock();
checkXIDCounter();
}
}
```

检查XID文件是否合法，读取XID\_FILE\_HEADER中的xidcounter，根据它计算文件的理论长度，对比实际长度

```java
     /**
     * 检查XID文件是否合法
     * 读取XID_FILE_HEADER中的xidcounter，根据它计算文件的理论长度，对比实际长度
     */
    private void checkXIDCounter() {
        long fileLen = 0;
        try {
fileLen = file.length(); //返回文件的字节数
        } catch (IOException e1) {
            Panic.panic(Error.BadXIDFileException);
        }
        if(fileLen < LEN_XID_HEADER_LENGTH) {
            Panic.panic(Error.BadXIDFileException);
        }

        ByteBuffer buf = ByteBuffer.allocate(LEN_XID_HEADER_LENGTH);
        try {
            fc.position(0);
            fc.read(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        this.xidCounter = Parser.parseLong(buf.array());
long end = getXidPosition(this.xidCounter + 1); //计算理论的字节长度
        if(end != fileLen) {
            Panic.panic(Error.BadXIDFileException);
        }
    }
```

计算理论的字节长度（当前 xid 状态所在位置）

```java
    // 根据事务xid取得其在xid文件中对应的位置
    private long getXidPosition(long xid) {
        return LEN_XID_HEADER_LENGTH + (xid-1)*XID_FIELD_SIZE;
    }
```

通过`getXidPosition(long xid) `方法获取当前xid状态在文件中的位置，并更新xid的状态为 `status`

```java
    // 更新xid事务的状态为status
    private void updateXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        byte[] tmp = new byte[XID_FIELD_SIZE];
        tmp[0] = status;
        ByteBuffer buf = ByteBuffer.wrap(tmp);
        try {
            fc.position(offset);
            fc.write(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
```

更新完 xid 的状态后还需要更新 xid 文件的头部

```java
    // 将XID加一，并更新XID Header
    private void incrXIDCounter() {
        xidCounter ++;
        ByteBuffer buf = ByteBuffer.wrap(Parser.long2Byte(xidCounter));
        try {
            fc.position(0);
            fc.write(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
```

开始事务：先使用 ReentrantLock 上锁，将当前 xid 的状态记录为 0 ，并增加head头部的xid信息，最后释放锁。

```java
    @Override
    public long begin() {
        counterLock.lock();
        try {
            long xid = xidCounter + 1;
            updateXID(xid, FIELD_TRAN_ACTIVE);
            incrXIDCounter();
            return xid;
        } finally {
            counterLock.unlock();
        }
    }
```

提交操作和撤销操作同理，只需要修改当前 xid 的状态即可

```java
    @Override
    public void commit(long xid) {
        updateXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public void abort(long xid) {
        updateXID(xid, FIELD_TRAN_ABORTED);
    }
```

检测XID事务是否处于status状态，先用`getXidPosition()`方法找到当前 xid 状态存放的位置，FeilChannal 移动到状态位置处，读取当前状态，再与`status`比较

```java
    // 检测XID事务是否处于status状态
    private boolean checkXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        ByteBuffer buf = ByteBuffer.wrap(new byte[XID_FIELD_SIZE]);
        try {
            fc.position(offset);
            fc.read(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return buf.array()[0] == status;
    }
```

检测是否是正在进行、已提交、撤回同理，只需要调用`checkXID()`方法传入对应的参数即可

```java
    @Override
    public boolean isActive(long xid) {
        if(xid == SUPER_XID) {
            return false;
        }
        return checkXID(xid, FIELD_TRAN_ACTIVE);
    }

    @Override
    public boolean isCommitted(long xid) {
        if(xid == SUPER_XID) {
            return true;
        }
        return checkXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public boolean isAborted(long xid) {
        if(xid == SUPER_XID) {
            return false;
        }
        return checkXID(xid, FIELD_TRAN_ABORTED);
    }
```

最后就是关闭操作了

```java
    @Override
    public void close() {
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
```

### 测试代码

```java
public class TransactionManagerTest {

    static Random random = new SecureRandom();

    private int transCnt = 0;
    private int noWorkers = 50;
    private int noWorks = 3000;
    private Lock lock = new ReentrantLock();
    private TransactionManager tmger;
    private Map<Long, Byte> transMap;
    private CountDownLatch cdl;


    @Test
    public void testMultiThread() {
tmger = TransactionManager.create("E:\\学习\\test\\tmp\\tranmger_test");
        transMap = new ConcurrentHashMap<>();
        cdl = new CountDownLatch(noWorkers);
        for(int i = 0; i < noWorkers; i ++) {
            Runnable r = () -> worker();
            new Thread(r).run();
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //tmger.close();
assert new File("E:\\学习\\test\\tmp\\tranmger_test.xid").delete();
    }

    private void worker() {
        boolean inTrans = false;
        long transXID = 0;
        for(int i = 0; i < noWorks; i ++) {
            int op = Math.abs(random.nextInt(6));
            if(op == 0) {
                lock.lock();
                if(inTrans == false) {
                    long xid = tmger.begin();
                    transMap.put(xid, (byte)0);
                    transCnt ++;
                    transXID = xid;
                    inTrans = true;
                } else {
                    int status = (random.nextInt(Integer.MAX_VALUE) % 2) + 1;
                    switch(status) {
                        case 1:
                            tmger.commit(transXID);
                            break;
                        case 2:
                            tmger.abort(transXID);
                            break;
                    }
                    transMap.put(transXID, (byte)status);
                    inTrans = false;
                }
                lock.unlock();
            } else {
                lock.lock();
                if(transCnt > 0) {
long xid=(long)((random.nextInt(Integer.MAX_VALUE)%transCnt) + 1);
                    byte status = transMap.get(xid);
                    boolean ok = false;
                    switch (status) {
                        case 0:
                            ok = tmger.isActive(xid);
                            break;
                        case 1:
                            ok = tmger.isCommitted(xid);
                            break;
                        case 2:
                            ok = tmger.isAborted(xid);
                            break;
                    }
                    assert ok;
                }
                lock.unlock();
            }
        }
        cdl.countDown();
    }
}
```

