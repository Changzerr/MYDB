package cn.changzer.mydb.backend.tm;

/**
 * @author lingqu
 * @date 2022/8/23
 * @apiNote
 */
public interface TransactionManager {
    long begin();                   //开启一个新事务
    void commit(long xid);          //提交一个事务
    void abort(long xid);           //取消一个事务
    boolean isActive(long xid);     //查询一个事务是否正在进行的状态
    boolean isCommitted(long xid);  //查询一个事务的状态是否提交
    boolean isAborted(long xid);    //查询一个事务的状态是否已取消
    void close(long xid);           //关闭TM
}
