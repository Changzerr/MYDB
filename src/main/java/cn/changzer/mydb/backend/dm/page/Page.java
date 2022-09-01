package cn.changzer.mydb.backend.dm.page;

/**
 * @author lingqu
 * @date 2022/8/28
 * @apiNote
 */
public interface Page {
    void lock();
    void unlock();
    void release();
    void setDirty(boolean dirty);
    boolean isDirty();
    int getPageNumber();
    byte[] getData();
}
