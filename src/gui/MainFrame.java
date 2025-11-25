package gui;



import dao.Database;
import dao.ProductDao;
import model.Product;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JLabel;


    public class MainFrame extends JFrame {

        private Database<Product, Integer> db;   // текущая открытая БД
        private JTable table;
        private ProductTableModel tableModel;
        private JLabel statusLabel;

        // Имена полей, которые понимает ProductDao.matchesField(...)
        private static final String[] PRODUCT_FIELDS = {
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
        private static final Color HEADER_LILAC = new Color(220, 200, 255); // сиреневый
        private static final Color HEADER_TEXT  = Color.BLACK;
        private static final Color ROW_PINK     = new Color(255, 228, 240);
        public MainFrame() {
            super("Wildberries Products DB");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(1100, 700);
            setLocationRelativeTo(null);

            buildMenu();
            buildContent();

            updateStatus();
        }

        /*  Построение GUI  */

        private void buildMenu() {
            JMenuBar mb = new JMenuBar();

            JMenu fileMenu = new JMenu("Файл");
            JMenuItem miNew = new JMenuItem("Создать БД...");
            JMenuItem miOpen = new JMenuItem("Открыть БД...");
            JMenuItem miSave = new JMenuItem("Сохранить индекс");
            JMenuItem miClose = new JMenuItem("Закрыть БД");
            JMenuItem miBackup = new JMenuItem("Создать backup");
            JMenuItem miRestore = new JMenuItem("Восстановить из backup...");
            JMenuItem miDelete = new JMenuItem("Удалить БД");
            JMenuItem miExit = new JMenuItem("Выход");

            miNew.addActionListener(e -> onCreateDB());
            miOpen.addActionListener(e -> onOpenDB());
            miSave.addActionListener(e -> onSaveDB());
            miClose.addActionListener(e -> onCloseDB());
            miBackup.addActionListener(e -> onBackup());
            miRestore.addActionListener(e -> onRestore());
            miDelete.addActionListener(e -> onDeleteDB());
            miExit.addActionListener(e -> System.exit(0));

            fileMenu.add(miNew);
            fileMenu.add(miOpen);
            fileMenu.addSeparator();
            fileMenu.add(miSave);
            fileMenu.add(miClose);
            fileMenu.addSeparator();
            fileMenu.add(miBackup);
            fileMenu.add(miRestore);
            fileMenu.addSeparator();
            fileMenu.add(miDelete);
            fileMenu.addSeparator();
            fileMenu.add(miExit);

            JMenu dataMenu = new JMenu("Данные");
            JMenuItem miAdd = new JMenuItem("Добавить запись...");
            JMenuItem miEdit = new JMenuItem("Редактировать выбранную...");
            JMenuItem miSearch = new JMenuItem("Поиск по полю...");
            JMenuItem miDeleteByField = new JMenuItem("Удалить по полю...");
            JMenuItem miClear = new JMenuItem("Очистить БД");

            miAdd.addActionListener(e -> onAddRecord());
            miEdit.addActionListener(e -> onEditSelected());
            miSearch.addActionListener(e -> onSearch());
            miDeleteByField.addActionListener(e -> onDeleteByField());
            miClear.addActionListener(e -> onClearDB());

            dataMenu.add(miAdd);
            dataMenu.add(miEdit);
            dataMenu.add(miSearch);
            dataMenu.add(miDeleteByField);
            dataMenu.addSeparator();
            dataMenu.add(miClear);

            JMenu exportMenu = new JMenu("Экспорт");
            JMenuItem miExportXlsx = new JMenuItem("Экспорт в XLSX...");
            miExportXlsx.addActionListener(e -> onExportXlsx());
            exportMenu.add(miExportXlsx);

            mb.add(fileMenu);
            mb.add(dataMenu);
            mb.add(exportMenu);

            setJMenuBar(mb);
        }

        private void buildContent() {
            // Верхняя панель с кнопками (дублируют часть меню)
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton bAdd = new JButton("Добавить");
            JButton bEdit = new JButton("Редактировать");
            JButton bSearch = new JButton("Поиск");
            JButton bDeleteByField = new JButton("Удалить по полю");

            bAdd.addActionListener(e -> onAddRecord());
            bEdit.addActionListener(e -> onEditSelected());
            bSearch.addActionListener(e -> onSearch());
            bDeleteByField.addActionListener(e -> onDeleteByField());

            top.add(bAdd);
            top.add(bEdit);
            top.add(bSearch);
            top.add(bDeleteByField);

            add(top, BorderLayout.NORTH);

            tableModel = new ProductTableModel();
            table = new JTable(tableModel) {
                @Override
                public Component prepareRenderer(
                        TableCellRenderer renderer,
                        int row, int column) {

                    Component c = super.prepareRenderer(renderer, row, column);

                    // если строка выделена — оставляем стандартную подсветку
                    if (isRowSelected(row)) {
                        return c;
                    }

                    // все строки данных — розовые
                    c.setBackground(new Color(255, 228, 240)); // нежно-розовый

                    return c;
                }
            };
            table.setAutoCreateRowSorter(true);
            table.setFillsViewportHeight(true);
            JTableHeader header = table.getTableHeader();
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row, int column) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);
                    lbl.setBackground(HEADER_LILAC);  // <-- СЮДА ЗАЛИВАЕМ СИРЕНЕВЫМ
                    lbl.setForeground(HEADER_TEXT);
                    lbl.setHorizontalAlignment(CENTER);
                    lbl.setOpaque(true);              // ОБЯЗАТЕЛЬНО, иначе фон не рисуется
                    return lbl;
                }
            });
            table.setAutoCreateRowSorter(true);
            table.setFillsViewportHeight(true);
            add(new JScrollPane(table), BorderLayout.CENTER);


            statusLabel = new JLabel("БД не открыта");
            add(statusLabel, BorderLayout.SOUTH);
        }

        /* Работа с БД (обработчики) */

        private void onCreateDB() {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Создать файл БД");
            fc.setFileFilter(new FileNameExtensionFilter("Файлы БД (*.dat)", "dat"));

            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dataFile = ensureExt(fc.getSelectedFile(), ".dat");
                Path dataPath = dataFile.toPath();
                Path indexPath = getIndexPathForData(dataPath);

                try {
                    if (db != null && db.isOpen()) db.close();
                    db = new ProductDao(dataPath, indexPath);
                    db.create();
                    reloadTable();
                    JOptionPane.showMessageDialog(this, "Новая база создана:\n" + dataPath);
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }

        private void onOpenDB() {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Открыть файл БД");
            fc.setFileFilter(new FileNameExtensionFilter("Файлы БД (*.dat)", "dat"));

            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dataFile = fc.getSelectedFile();
                Path dataPath = dataFile.toPath();
                Path indexPath = getIndexPathForData(dataPath);

                try {
                    if (db != null && db.isOpen()) db.close();
                    db = new ProductDao(dataPath, indexPath);
                    db.open();
                    reloadTable();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }

        private void onSaveDB() {
            if (!ensureDbOpen()) return;
            try {
                db.save(); // в твоём DAO это сохранит индекс
                JOptionPane.showMessageDialog(this, "Индекс/состояние сохранено.");
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onCloseDB() {
            if (db == null || !db.isOpen()) return;
            try {
                db.close();
                db = null;
                tableModel.setProducts(new ArrayList<>());
                updateStatus();
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onBackup() {
            if (!ensureDbOpen()) return;

            try {
                // backup в каталог "backup" рядом с файлом БД
                Path dbPath = db.getDatabasePath().toAbsolutePath();
                Path dir = dbPath.getParent() != null ? dbPath.getParent() : Paths.get(".");
                Path backupDir = dir.resolve("backup");
                Files.createDirectories(backupDir);

                db.backup(backupDir);
                JOptionPane.showMessageDialog(this, "Backup создан в:\n" + backupDir.toAbsolutePath());
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onRestore() {
            if (!ensureDbOpen()) return;

            // Выбираем каталог backup-а
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Выберите папку с backup-файлами");

            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

            File dir = fc.getSelectedFile();
            try {
                db.restore(dir.toPath());
                reloadTable();
                JOptionPane.showMessageDialog(this, "База восстановлена из:\n" + dir.getAbsolutePath());
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onDeleteDB() {
            if (!ensureDbOpen()) return;
            int opt = JOptionPane.showConfirmDialog(this,
                    "Удалить файлы БД с диска?",
                    "Подтверждение удаления",
                    JOptionPane.OK_CANCEL_OPTION);
            if (opt != JOptionPane.OK_OPTION) return;

            try {
                db.delete();
                db = null;
                tableModel.setProducts(new ArrayList<>());
                updateStatus();
                JOptionPane.showMessageDialog(this, "Файлы БД удалены.");
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onClearDB() {
            if (!ensureDbOpen()) return;
            int opt = JOptionPane.showConfirmDialog(this,
                    "Очистить все записи в БД?",
                    "Подтверждение очистки",
                    JOptionPane.OK_CANCEL_OPTION);
            if (opt != JOptionPane.OK_OPTION) return;

            try {
                db.clear();
                reloadTable();
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onAddRecord() {
            if (!ensureDbOpen()) return;

            Product p = ProductDialog.showDialog(this, null);
            if (p == null) return;

            String error = p.getValidationError();
            if (error != null) {
                JOptionPane.showMessageDialog(this, "Ошибка валидации: " + error,
                        "Неверные данные", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                boolean ok = db.insert(p);
                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                            "Товар с таким ID уже существует.",
                            "Ошибка уникальности ключа",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                reloadTable();
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onEditSelected() {
            if (!ensureDbOpen()) return;

            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Выберите строку для редактирования.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            Product current = tableModel.getProductAt(modelRow);
            if (current == null) return;

            int key = current.getProduct_id();

            try {
                Optional<Product> fromDb = db.findByKey(key);
                if (fromDb.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Запись с ID=" + key + " не найдена в файле (индекс устарел?)");
                    reloadTable();
                    return;
                }

                Product edited = ProductDialog.showDialog(this, fromDb.get());
                if (edited == null) return;

                String error = edited.getValidationError();
                if (error != null) {
                    JOptionPane.showMessageDialog(this, "Ошибка валидации: " + error,
                            "Неверные данные", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // ключ менять нельзя: DAO пишет newRecord с тем же ключом
                edited.setProduct_id(key);

                boolean ok = db.updateByKey(key, edited);
                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                            "Не удалось обновить запись с ID=" + key,
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                } else {
                    reloadTable();
                }

            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onSearch() {
            if (!ensureDbOpen()) return;

            SearchDialog.Result r = SearchDialog.showDialog(this,
                    "Поиск по полю", PRODUCT_FIELDS);
            if (r == null) return;

            try {
                List<Product> result = db.findByField(r.fieldName, r.value);
                showSearchResults(result);
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onDeleteByField() {
            if (!ensureDbOpen()) return;

            SearchDialog.Result r = SearchDialog.showDialog(this,
                    "Удаление по полю", PRODUCT_FIELDS);
            if (r == null) return;

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Удалить записи, где " + r.fieldName + " = " + r.value + "?",
                    "Подтверждение удаления",
                    JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION) return;

            try {
                int deleted = db.deleteByField(r.fieldName, r.value);
                reloadTable();
                JOptionPane.showMessageDialog(this,
                        "Удалено записей: " + deleted);
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void onExportXlsx() {
            if (!ensureDbOpen()) return;

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Экспорт в XLSX");
            fc.setFileFilter(new FileNameExtensionFilter("Excel файлы (*.xlsx)", "xlsx"));
            fc.setSelectedFile(new File("products.xlsx"));

            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            File f = ensureExt(fc.getSelectedFile(), ".xlsx");
            try {
                System.out.println("Exporting XLSX to: " + f.getAbsolutePath()); // лог в консоль
                db.exportToXlsx(f.toPath());
                JOptionPane.showMessageDialog(this,
                        "Экспортировано в:\n" + f.getAbsolutePath(),
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace(); // важно, чтобы видеть стек-трейс
                showError(ex);        // твой диалог с сообщением
            }
        }


        /* ====================== Вспомогательные ====================== */

        private boolean ensureDbOpen() {
            if (db == null || !db.isOpen()) {
                JOptionPane.showMessageDialog(this,
                        "База данных не открыта.",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }

        private void reloadTable() {
            if (db == null || !db.isOpen()) {
                tableModel.setProducts(new ArrayList<>());
            } else {
                try {
                    List<Product> all = db.findAll(); // это всего лишь визуализация — по заданию можно
                    tableModel.setProducts(all);
                } catch (Exception ex) {
                    showError(ex);
                }
            }
            updateStatus();
        }

        private void updateStatus() {
            if (db == null || !db.isOpen()) {
                statusLabel.setText("БД не открыта");
                setTitle("Wildberries Products DB");
            } else {
                statusLabel.setText("Файл: " + db.getDatabasePath().toAbsolutePath()
                        + " | записей: " + db.getRecordCount());
                setTitle("Wildberries Products DB — " + db.getDatabasePath().getFileName());
            }
        }

        private void showSearchResults(List<Product> products) {
            if (products.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ничего не найдено.");
                return;
            }
            String[] columns = {
                    "ID товара", "ID продавца", "Название",
                    "Цена", "Бренд", "Артикул",
                    "Количество", "Категория", "Описание"
            };
            Object[][] data = new Object[products.size()][columns.length];
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                data[i][0] = p.getProduct_id();
                data[i][1] = p.getSeller_id();
                data[i][2] = p.getName();
                data[i][3] = p.getPrice();
                data[i][4] = p.getBrand();
                data[i][5] = p.getArticle();
                data[i][6] = p.getStock_quantity();
                data[i][7] = p.getCategory();
                data[i][8] = p.getDescription();
            }
            JTable t = new JTable(data, columns);
            JScrollPane sp = new JScrollPane(t);
            sp.setPreferredSize(new Dimension(900, 400));
            JOptionPane.showMessageDialog(this, sp,
                    "Результаты поиска: " + products.size(),
                    JOptionPane.PLAIN_MESSAGE);
        }

        private File ensureExt(File f, String ext) {
            String name = f.getName().toLowerCase();
            if (!name.endsWith(ext)) {
                return new File(f.getParentFile(), f.getName() + ext);
            }
            return f;
        }

        private Path getIndexPathForData(Path dataPath) {
            String name = dataPath.getFileName().toString();
            if (name.endsWith(".dat")) {
                name = name.substring(0, name.length() - 4);
            }
            return dataPath.getParent().resolve(name + ".idx");
        }

        private void showError(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    ex.toString(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }

        /* Табличная модель  */

        private static class ProductTableModel extends AbstractTableModel {
            private final String[] columns = {
                    "ID товара", "ID продавца", "Название",
                    "Цена", "Бренд", "Артикул",
                    "Количество", "Категория", "Описание"
            };
            private final Class<?>[] classes = {
                    Integer.class, Integer.class, String.class,
                    Double.class, String.class, Integer.class,
                    Integer.class, String.class, String.class
            };

            private List<Product> products = new ArrayList<>();

            public void setProducts(List<Product> products) {
                this.products = products;
                fireTableDataChanged();
            }

            public Product getProductAt(int row) {
                if (row < 0 || row >= products.size()) return null;
                return products.get(row);
            }

            @Override
            public int getRowCount() {
                return products.size();
            }

            @Override
            public int getColumnCount() {
                return columns.length;
            }

            @Override
            public String getColumnName(int column) {
                return columns[column];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return classes[columnIndex];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Product p = products.get(rowIndex);
                switch (columnIndex) {
                    case 0: return p.getProduct_id();
                    case 1: return p.getSeller_id();
                    case 2: return p.getName();
                    case 3: return p.getPrice();
                    case 4: return p.getBrand();
                    case 5: return p.getArticle();
                    case 6: return p.getStock_quantity();
                    case 7: return p.getCategory();
                    case 8: return p.getDescription();
                }
                return null;
            }
        }
    }


