package cn.changzer.mydb.backend.utils;

/**
 * @author lingqu
 * @date 2022/8/24
 * @apiNote
 */
public class Panic {
    public static void panic(Exception err){
        err.printStackTrace();
        System.exit(1);
    }
}
