package cn.changzer.mydb.backend.dm.pageCache;

import cn.changzer.mydb.backend.dm.page.Page;
import cn.changzer.mydb.backend.utils.Panic;
import cn.changzer.mydb.common.Error;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * @author lingqu
 * @date 2022/8/28
 * @apiNote
 */
public interface PageCache {
    public static final int PAGE_SIZE = 1 << 13;

    int newPage(byte[] initData);
    Page getPage(int pgno) throws Exception;
    void close();
    void release(Page page);

    void truncateByBgno(int maxPgno);
    int getPageNumber();
    void flushPage(Page pg);

    public static PageCacheImpl create(String path, long memory) {
        File f = new File(path+PageCacheImpl.DB_SUFFIX);
        try{
            if(!f.createNewFile()){
                Panic.panic(Error.FileExistsException);
            }
        }catch(Exception e){
            Panic.panic(e);
        }

        if(!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f,"rw");
            fc = raf.getChannel();
        }catch(Exception e){
            Panic.panic(e);
        }
        return new PageCacheImpl(raf,fc,(int)memory/PAGE_SIZE);
    }

    public static PageCacheImpl open(String path, long memory) {
        File f = new File(path+PageCacheImpl.DB_SUFFIX);
        if(!f.exists()) {
            Panic.panic(Error.FileNotExistsException);
        }

        if(!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f,"rw");
            fc = raf.getChannel();
        }catch(Exception e){
            Panic.panic(e);
        }
        return new PageCacheImpl(raf,fc,(int)memory/PAGE_SIZE);
    }
}
