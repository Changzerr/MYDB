# MYDB

参考：https://github.com/CN-GuoZiyang/MYDB

MYDB 是一个 Java 实现的简单的数据库，部分原理参照自 MySQL、PostgreSQL 和 SQLite。实现了以下功能：

- 数据的可靠性和数据恢复

- 两段锁协议（2PL）实现可串行化调度

- MVCC

- 两种事务隔离级别（读提交和可重复读）

- 死锁处理

- 简单的表和字段管理

- 简陋的 SQL 解析（因为懒得写词法分析和自动机，就弄得比较简陋）

- 基于 socket 的 server 和 client

   

​	MYDB 分为后端和前端，前后端通过 socket 进行交互。前端（客户端）的职责很单一，读取用户输入，并发送到后端执行，输出返回结果，并等待下一次输入。MYDB 后端则需要解析 SQL，如果是合法的 SQL，就尝试执行并返回结果。不包括解析器，MYDB 的后端划分为五个模块，每个模块都又一定的职责，通过接口向其依赖的模块提供方法。五个模块如下：

1. Transaction Manager（TM） 

   > TM 通过维护 XID 文件来维护事务的状态，并提供接口供其他模块来查询某个事务的状态。 

2. Data Manager（DM）

   > DM 直接管理数据库 DB 文件和日志文件。DM 的主要职责有：1) 分页管理 DB 文件，并进行缓存；2) 管	理日志文件，保证在发生错误时可以根据日志进行恢复；3) 抽象 DB 文件为 DataItem 供上层模块使用，	并提供缓存。

3. Version Manager（VM） 

   > VM 基于两段锁协议实现了调度序列的可串行化，并实现了 MVCC 以消除读写阻塞。同时实现了两种隔离	级别。

4. Index Manager（IM） 

   > IM 实现了基于 B+ 树的索引，BTW，目前 where 只支持已索引字段。

5. Table Manager（TBM）

   > TBM 实现了对字段和表的管理。同时，解析 SQL 语句，并根据语句操作表。



总结一下各个模块提供的操作 :

- TM: begin, commit(T), abort(T), isActive(T),isCommitted(T),isAborted(T) 

	> TM提供了针对事务的开始, 提交, 回滚操作, 同时提供了对数据项状态的查询操作. 

- DM: insert(x), update(x), read(x) 

	> DM提供了针对数据项(data item)的基本插入, 更新, 读取操作, 且这些操作是原子性的. DM会直接对数据库文件进行读写.

- VM: insert(X), update(X), read(X), delete(X) 

	> VM提供了针对记录(entry)的增删查改操作, VM在内部为每条记录维护多个版本, 并根据不同的事务, 返回不同的版本. VM对这些实现, 是建立在DM和TM的各个操作上的，还有一个事务可见性类Visibility。 

- IM: value search(key), insert(key, value) 

	> IM提供了对索引的基本操作.

- TBM: execute(statement) 

	> TBM就是非常高层的模块了, 他能直接执行用户输入的语句(statement), 然后进行执行. TBM对语句的执行是建立在VM和IM提供的各个操作上的. 





**read语句的流程** 

假设现在要执行read * from student where id = 2012141461290, 并且在id上已经建有索引. 执行过程如下: 

1、TBM接受语句, 并进行解析. 

2、TBM调用IM的search方法, 查找对应记录所在的地址. 

3、TBM调用VM的read方法, 并将地址作为参数, 从VM中尝试读取记录内容. 

4、VM通过DM的read操作, 读取该条记录的最新版本. 

5、VM检测该版本是否对该事务可见, 其中需要Visibility.isVisible()方法. 

6、如果可见, 则返回该版本的数据. 

7、如果不可见, 则读取上一个版本, 并重复5, 6, 7. 

8、TBM取得记录的二进制内容后, 对其进行解析, 还原出记录内容.

9、TBM将记录的内容返回给客户端. 

**insert语句的流程** 

假设现在要执行insert into student values “zhangyuanjia” 2012141461290这条语句. 执行过程如下: 

1、TBM接受语句, 并进行解析. 

2、TBM将values的值, 二进制化. 

3、TBM利用VM的insert操作, 将二进制化后的数据, 插入到数据库. 

4、VM为该条数据建立版本控制, 并利用DM的insert操作, 将数据插入到数据库. 

5、DM将数据插入 到数据库, 并返回其被存储的地址. 

6、VM将得到的地址, 作为该条记录的handler, 返回给TBM. 

7、TBM计算该条语 句的key, 并将handler作为data, 并调用IM的insert, 建立索引. 

8、IM利用DM提供的read和insert等操作, 将key和data存入 索引中. 

9、TBM返回客户端插入成功的信息.
