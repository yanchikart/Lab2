// src/dao/Database.java
package dao;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface Database<T, K> extends Closeable {


    void create() throws IOException;
    void open() throws IOException;
    void save() throws IOException;
    void clear() throws IOException;
    void delete() throws IOException;
    @Override
    void close() throws IOException;


    boolean insert(T record) throws IOException;
    boolean updateByKey(K key, T newRecord) throws IOException;
    int deleteByKey(K key) throws IOException;
    int deleteByField(String fieldName, Object value) throws IOException;
    Optional<T> findByKey(K key) throws IOException;
    List<T> findByField(String fieldName, Object value) throws IOException;
    List<T> findAll() throws IOException;


    void backup(Path backupDir) throws IOException;
    void restore(Path backupDir) throws IOException;
    void exportToXlsx(Path targetFile) throws IOException;


    boolean isOpen();
    int getRecordCount();
    Path getDatabasePath();
}