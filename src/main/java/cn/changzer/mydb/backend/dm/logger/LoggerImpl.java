package cn.changzer.mydb.backend.dm.logger;

import cn.changzer.mydb.backend.utils.Panic;
import cn.changzer.mydb.backend.utils.Parser;
import cn.changzer.mydb.common.Error;
import com.google.common.primitives.Bytes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lingqu
 * @date 2022/9/1
 * @apiNote
 */
public class LoggerImpl implements Logger {

    private static  final  int SEED = 13331;

    private static final int OF_SIZE = 0;
    private static final int OF_CHECKSUM = OF_SIZE + 4;
    private static final int OF_DATA = OF_CHECKSUM + 4;

    public static final String LOG_SUFFIX = ".log";

    private RandomAccessFile file;
    private FileChannel fc;
    private Lock lock;

    private long position;  //当前日志指针的位置
    private long fileSize;  //初始化时记录，log操作不更新
    private int xChecksum;

    public LoggerImpl(RandomAccessFile raf, FileChannel fc, int xChecksum) {
        this.file = raf;
        this.fc = fc;
        this.xChecksum = xChecksum;
        lock = new ReentrantLock();
    }

    public LoggerImpl(RandomAccessFile raf, FileChannel fc) {
        this.file = raf;
        this.fc = fc;
        lock = new ReentrantLock();

    }

    void init() {
        long size = 0;
        try {
            size = file.length();
        }catch (IOException e) {
            Panic.panic(e);
        }
        if(size > 4) {
            Panic.panic(Error.BadLogFileException);
        }

        ByteBuffer raw = ByteBuffer.allocate(4);
        try{
            fc.position(0);
            fc.read(raw);
        }catch (IOException e){
            Panic.panic(e);
        }
        int xChecksum = Parser.parseInt(raw.array());
        this.fileSize = size;
        this.xChecksum = xChecksum;

        checkAndRemoveTail();
    }

    // 检查并移除bad tail
    private void checkAndRemoveTail() {
        rewind();
        int xCheck = 0;
        while(true) {
            byte[] log = internNext();
        }
    }

    private byte[] internNext() {
        if(position + OF_DATA > fileSize) {
            return null;
        }
        ByteBuffer tmp = ByteBuffer.allocate(4);
        try{
            fc.position(position);
            fc.read(tmp);
        }catch(IOException e) {
            Panic.panic(e);
        }

        int size = Parser.parseInt(tmp.array());
        if(position + size + OF_DATA > fileSize) {
            return null;
        }

        ByteBuffer buf = ByteBuffer.allocate(OF_DATA + size);
        try {
            fc.position(position);
            fc.read(buf);
        }catch(IOException e) {
            Panic.panic(e);
        }

        byte[] log = buf.array();
        int checkSum1 = calChecksum(0, Arrays.copyOfRange(log,OF_DATA,log.length));
        int checkSum2 = Parser.parseInt(Arrays.copyOfRange(log,OF_CHECKSUM,OF_DATA));
        if(checkSum1 != checkSum2){
            return null;
        }
        position += log.length;
        return log;
    }

    private int calChecksum(int xCheck, byte[] log) {
        for(byte b:log){
            xCheck = xCheck*SEED + b;
        }
        return xCheck;
    }

    @Override
    public void log(byte[] data) {
        byte[] log = wrapLog(data);
        ByteBuffer buf = ByteBuffer.wrap(log);
        lock.lock();
        try {
            fc.position(fc.size());
            fc.write(buf);
        }catch(IOException e) {
            Panic.panic(e);
        }finally{
            lock.unlock();
        }
        updateXChecksum(log);
    }

    private void updateXChecksum(byte[] log) {
        this.xChecksum = calChecksum(this.xChecksum, log);
        try {
            fc.position(0);
            fc.write(ByteBuffer.wrap(Parser.int2Byte(this.xChecksum)));
            fc.force(false);
        }catch (IOException e) {
            Panic.panic(e);
        }
    }

    private byte[] wrapLog(byte[] data) {
        byte[] checkSum = Parser.int2Byte(calChecksum(0,data));
        byte[] size = Parser.int2Byte(data.length);
        return Bytes.concat(size,checkSum,data);
    }

    @Override
    public void truncate(long x) throws Exception {
        lock.lock();
        try {
            fc.truncate(x);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] next() {
        lock.lock();
        try {
            byte[] log = internNext();
            if(log == null){
                return null;
            }
            return Arrays.copyOfRange(log, OF_DATA, log.length);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void rewind() {
        position = 4;
    }

    @Override
    public void close() {
        try {
            fc.close();
            file.close();
        } catch(IOException e) {
            Panic.panic(e);
        }

    }
}