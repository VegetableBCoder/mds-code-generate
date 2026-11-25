package com.mds.codegen.read;

/** 读取 DAO 层失败（找不到文件、结构不符合约定、信息有歧义……）。 */
public class ReaderException extends RuntimeException {

    public ReaderException(String message) {
        super(message);
    }

    public ReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
