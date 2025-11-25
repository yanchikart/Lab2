package dao;

import model.Product;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class ProductDao implements Database<Product, Integer> {

    private final Path dataPath;
    private final Path indexPath;

    private RandomAccessFile dataFile;
    // индекс id -> смещение в файле .dat
    private final Map<Integer, Long> index = new HashMap<>();

    private boolean open;
    private int recordCount; // количество НЕудалённых записей

    public ProductDao(Path dataPath, Path indexPath) {
        this.dataPath = dataPath;
        this.indexPath = indexPath;
    }

    public ProductDao(String dataFileName, String indexFileName) {
        this(Paths.get(dataFileName), Paths.get(indexFileName));
    }

    /* ==================== Работа с файлами БД ==================== */

    @Override
    public void create() throws IOException {
        if (dataPath.getParent() != null) {
            Files.createDirectories(dataPath.getParent());
        }
        if (indexPath.getParent() != null) {
            Files.createDirectories(indexPath.getParent());
        }

        dataFile = new RandomAccessFile(dataPath.toFile(), "rw");
        dataFile.setLength(0);      // очищаем
        open = true;

        index.clear();
        recordCount = 0;
        saveIndex();                // создаём/очищаем индекс-файл
    }

    @Override
    public void open() throws IOException {
        if (!Files.exists(dataPath)) {
            throw new FileNotFoundException("Data file not found: " + dataPath);
        }
        dataFile = new RandomAccessFile(dataPath.toFile(), "rw");
        open = true;

        loadIndexOrRebuild();
    }

    private void loadIndexOrRebuild() throws IOException {
        index.clear();
        if (Files.exists(indexPath)) {
            // читаем индекс из файла
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(indexPath)))) {
                while (true) {
                    int id = in.readInt();
                    long offset = in.readLong();
                    index.put(id, offset);
                }
            } catch (EOFException e) {
                // нормальное завершение чтения файла
            }
            recordCount = index.size();
        } else {
            // если индекс-файла нет — восстанавливаем индекс, просканировав .dat
            rebuildIndexFromData();
            saveIndex();
        }
    }

    private void rebuildIndexFromData() throws IOException {
        index.clear();
        dataFile.seek(0);
        long fileLength = dataFile.length();
        int count = 0;
        while (dataFile.getFilePointer() < fileLength) {
            long offset = dataFile.getFilePointer();
            boolean deleted = dataFile.readBoolean();
            Product p = readProduct(dataFile);
            if (!deleted) {
                index.put(p.getProduct_id(), offset);
                count++;
            }
        }
        recordCount = count;
    }

    @Override
    public void save() throws IOException {
        ensureOpen();
        // сами данные уже на диске, нужно только сохранить индекс
        saveIndex();
    }

    private void saveIndex() throws IOException {
        if (indexPath.getParent() != null) {
            Files.createDirectories(indexPath.getParent());
        }
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(indexPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)))) {

            for (Map.Entry<Integer, Long> e : index.entrySet()) {
                out.writeInt(e.getKey());
                out.writeLong(e.getValue());
            }
        }
    }

    @Override
    public void clear() throws IOException {
        ensureOpen();
        dataFile.setLength(0);  // очищаем .dat
        index.clear();          // очищаем индекс в памяти
        recordCount = 0;
        saveIndex();            // и на диске
    }

    @Override
    public void delete() throws IOException {
        close();
        Files.deleteIfExists(dataPath);
        Files.deleteIfExists(indexPath);
    }

    @Override
    public void close() throws IOException {
        if (dataFile != null) {
            saveIndex();
            dataFile.close();
            dataFile = null;
        }
        open = false;
    }

    private void ensureOpen() throws IOException {
        if (!open || dataFile == null) {
            throw new IOException("Database is not open");
        }
    }

    /* ==================== Операции с записями ==================== */

    @Override
    public synchronized boolean insert(Product record) throws IOException {
        ensureOpen();
        int id = record.getProduct_id();

        // проверка уникальности ключа
        if (index.containsKey(id)) {
            return false;
        }

        long offset = dataFile.length();   // пишем в конец файла
        dataFile.seek(offset);
        dataFile.writeBoolean(false);      // флаг deleted = false
        writeProduct(dataFile, record);

        index.put(id, offset);
        recordCount++;
        return true;
    }

    @Override
    public synchronized boolean updateByKey(Integer key, Product newRecord) throws IOException {
        ensureOpen();
        Long oldOffset = index.get(key);
        if (oldOffset == null) {
            return false;
        }

        // помечаем старую запись как удалённую
        markDeleted(oldOffset);
        index.remove(key);

        // гарантируем, что ключ у новой записи правильный
        newRecord.setProduct_id(key);

        // дописываем новую запись в конец
        long offset = dataFile.length();
        dataFile.seek(offset);
        dataFile.writeBoolean(false);
        writeProduct(dataFile, newRecord);

        index.put(key, offset);
        // recordCount не меняем: количество живых записей то же
        return true;
    }

    @Override
    public synchronized int deleteByKey(Integer key) throws IOException {
        ensureOpen();
        Long offset = index.remove(key);
        if (offset == null) {
            return 0;
        }
        markDeleted(offset);
        recordCount--;
        return 1;
    }

    @Override
    public synchronized int deleteByField(String fieldName, Object value) throws IOException {
        ensureOpen();
        int deletedCount = 0;

        dataFile.seek(0);
        long fileLength = dataFile.length();
        while (dataFile.getFilePointer() < fileLength) {
            long recordOffset = dataFile.getFilePointer();
            boolean deleted = dataFile.readBoolean();
            Product p = readProduct(dataFile);

            if (!deleted && matchesField(p, fieldName, value)) {
                markDeleted(recordOffset);
                index.remove(p.getProduct_id());
                deletedCount++;
            }
        }
        recordCount -= deletedCount;
        return deletedCount;
    }

    @Override
    public Optional<Product> findByKey(Integer key) throws IOException {
        ensureOpen();
        Long offset = index.get(key);
        if (offset == null) {
            return Optional.empty();
        }
        Product p = readAtOffset(offset);
        return Optional.ofNullable(p);
    }

    @Override
    public List<Product> findByField(String fieldName, Object value) throws IOException {
        ensureOpen();
        List<Product> result = new ArrayList<>();

        // поиск по ключу — через индекс
        if ("product_id".equals(fieldName)) {
            Integer key = toInt(value);
            return findByKey(key).map(List::of).orElseGet(List::of);
        }

        dataFile.seek(0);
        long fileLength = dataFile.length();
        while (dataFile.getFilePointer() < fileLength) {
            boolean deleted = dataFile.readBoolean();
            Product p = readProduct(dataFile);
            if (!deleted && matchesField(p, fieldName, value)) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public List<Product> findAll() throws IOException {
        ensureOpen();
        List<Product> all = new ArrayList<>();
        dataFile.seek(0);
        long fileLength = dataFile.length();
        while (dataFile.getFilePointer() < fileLength) {
            boolean deleted = dataFile.readBoolean();
            Product p = readProduct(dataFile);
            if (!deleted) {
                all.add(p);
            }
        }
        return all;
    }

    /*  backup / restore / export */

    @Override
    public void backup(Path backupDir) throws IOException {
        ensureOpen();
        Files.createDirectories(backupDir);

        // Сначала синхронизируем .idx на диске с актуальной картой index
        saveIndex(); // или просто save(), если он только индекс пишет

        Files.copy(dataPath, backupDir.resolve(dataPath.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);

        if (Files.exists(indexPath)) {
            Files.copy(indexPath, backupDir.resolve(indexPath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }


    @Override
    public void restore(Path backupDir) throws IOException {
        close(); // внутри у тебя уже saveIndex()

        Path backupData = backupDir.resolve(dataPath.getFileName());
        Path backupIdx  = backupDir.resolve(indexPath.getFileName());

        Files.copy(backupData, dataPath, StandardCopyOption.REPLACE_EXISTING);

        if (Files.exists(backupIdx)) {
            Files.copy(backupIdx, indexPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(indexPath); // чтобы потом пересобрать индекс из .dat
        }

        open(); // снова открываем и перечитываем индекс/recordCount
    }



    @Override
    public void exportToXlsx(Path targetFile) throws IOException {
        ensureOpen();

        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("Products");

            String[] headers = {
                    "product_id",
                    "seller_id",
                    "name",
                    "price",
                    "brand",
                    "article",
                    "stock_quantity",
                    "category",
                    "description"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            java.util.List<Product> products = findAll();

            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);

                int col = 0;
                row.createCell(col++).setCellValue(p.getProduct_id());
                row.createCell(col++).setCellValue(p.getSeller_id());
                row.createCell(col++).setCellValue(nullToEmpty(p.getName()));
                row.createCell(col++).setCellValue(p.getPrice());
                row.createCell(col++).setCellValue(nullToEmpty(p.getBrand()));
                row.createCell(col++).setCellValue(p.getArticle());
                row.createCell(col++).setCellValue(p.getStock_quantity());
                row.createCell(col++).setCellValue(nullToEmpty(p.getCategory()));
                row.createCell(col).setCellValue(nullToEmpty(p.getDescription()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            try (java.io.OutputStream out = Files.newOutputStream(targetFile)) {
                workbook.write(out);
            }
        } finally {
            workbook.close();
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }




    /* ==================== Реализация доп. методов интерфейса ==================== */

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public int getRecordCount() {
        return recordCount;
    }

    @Override
    public Path getDatabasePath() {
        return dataPath;
    }

    /* ==================== Вспомогательные методы ==================== */

    private void markDeleted(long offset) throws IOException {
        long currentPos = dataFile.getFilePointer(); // запоминаем, где были
        dataFile.seek(offset);
        dataFile.writeBoolean(true);                 // ставим deleted = true
        dataFile.seek(currentPos);                   // возвращаемся туда, где шёл цикл
    }

    private Product readAtOffset(long offset) throws IOException {
        long currentPos = dataFile.getFilePointer();
        try {
            dataFile.seek(offset);
            boolean deleted = dataFile.readBoolean();
            Product p = readProduct(dataFile);
            if (deleted) return null;
            return p;
        } finally {
            dataFile.seek(currentPos); // чтобы не ломать другие операции чтения
        }
    }


    /** Записываем запись Product в файл после флага deleted */
    private static void writeProduct(RandomAccessFile file, Product p) throws IOException {
        file.writeInt(p.getProduct_id());
        file.writeInt(p.getSeller_id());
        file.writeDouble(p.getPrice());
        file.writeInt(p.getArticle());
        file.writeInt(p.getStock_quantity());

        file.writeUTF(nonNull(p.getName()));
        file.writeUTF(nonNull(p.getBrand()));
        file.writeUTF(nonNull(p.getCategory()));
        file.writeUTF(nonNull(p.getDescription()));
    }

    /** Читаем запись Product из файла после флага deleted */
    private static Product readProduct(RandomAccessFile file) throws IOException {
        Product p = new Product();
        p.setProduct_id(file.readInt());
        p.setSeller_id(file.readInt());
        p.setPrice(file.readDouble());
        p.setArticle(file.readInt());
        p.setStock_quantity(file.readInt());

        p.setName(file.readUTF());
        p.setBrand(file.readUTF());
        p.setCategory(file.readUTF());
        p.setDescription(file.readUTF());
        return p;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static boolean matchesField(Product p, String fieldName, Object value) {
        String vStr = value == null ? null : value.toString();
        switch (fieldName) {
            case "product_id":
                return p.getProduct_id() == toInt(value);
            case "seller_id":
                return p.getSeller_id() == toInt(value);
            case "article":
                return p.getArticle() == toInt(value);
            case "stock_quantity":
                return p.getStock_quantity() == toInt(value);
            case "name":
                return Objects.equals(p.getName(), vStr);
            case "brand":
                return Objects.equals(p.getBrand(), vStr);
            case "category":
                return Objects.equals(p.getCategory(), vStr);
            case "description":
                return Objects.equals(p.getDescription(), vStr);
            case "price":
                return Double.compare(p.getPrice(), toDouble(value)) == 0;
            default:
                // неизвестное поле — ни с чем не совпадает
                return false;
        }
    }

    private static int toInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString());
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        return Double.parseDouble(o.toString());
    }
}
