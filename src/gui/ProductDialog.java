package gui;

import model.Product;

import javax.swing.*;
import java.awt.*;

public class ProductDialog extends JDialog {

    private JTextField tfId;
    private JTextField tfSellerId;
    private JTextField tfName;
    private JTextField tfPrice;
    private JTextField tfBrand;
    private JTextField tfArticle;
    private JTextField tfStock;
    private JTextField tfCategory;
    private JTextArea taDescription;

    private boolean okPressed = false;
    private Product result;

    // Здесь тоже owner = Frame
    public static Product showDialog(Frame owner, Product existing) {
        ProductDialog dlg = new ProductDialog(owner, existing);
        dlg.setVisible(true);
        return dlg.okPressed ? dlg.result : null;
    }

    private ProductDialog(Frame owner, Product existing) {
        super(owner, existing == null ? "Добавление товара" : "Редактирование товара", true);
        buildUI();
        if (existing != null) fillFromProduct(existing);
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;

        tfId = new JTextField(15);
        tfSellerId = new JTextField(15);
        tfName = new JTextField(25);
        tfPrice = new JTextField(15);
        tfBrand = new JTextField(20);
        tfArticle = new JTextField(15);
        tfStock = new JTextField(15);
        tfCategory = new JTextField(20);
        taDescription = new JTextArea(4, 25);
        taDescription.setLineWrap(true);
        taDescription.setWrapStyleWord(true);

        addRow(form, c, "ID товара (product_id, int):", tfId);
        addRow(form, c, "ID продавца (seller_id, int):", tfSellerId);
        addRow(form, c, "Название (name):", tfName);
        addRow(form, c, "Цена (price, double):", tfPrice);
        addRow(form, c, "Бренд (brand):", tfBrand);
        addRow(form, c, "Артикул (article, int):", tfArticle);
        addRow(form, c, "Количество (stock_quantity, int):", tfStock);
        addRow(form, c, "Категория (category):", tfCategory);

        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.gridy++;
        form.add(new JLabel("Описание (description):"), c);

        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        JScrollPane sp = new JScrollPane(taDescription);
        form.add(sp, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Отмена");
        buttons.add(ok);
        buttons.add(cancel);

        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> onCancel());

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints c, String label, JComponent field) {
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        panel.add(field, c);

        c.gridy++;
    }

    private void fillFromProduct(Product p) {
        tfId.setText(String.valueOf(p.getProduct_id()));
        tfSellerId.setText(String.valueOf(p.getSeller_id()));
        tfName.setText(p.getName());
        tfPrice.setText(String.valueOf(p.getPrice()));
        tfBrand.setText(p.getBrand());
        tfArticle.setText(String.valueOf(p.getArticle()));
        tfStock.setText(String.valueOf(p.getStock_quantity()));
        tfCategory.setText(p.getCategory());
        taDescription.setText(p.getDescription());
    }

    private void onOk() {
        try {
            Product p = new Product();
            p.setProduct_id(Integer.parseInt(tfId.getText().trim()));
            p.setSeller_id(Integer.parseInt(tfSellerId.getText().trim()));
            p.setName(tfName.getText());
            p.setPrice(Double.parseDouble(tfPrice.getText().trim()));
            p.setBrand(tfBrand.getText());
            p.setArticle(Integer.parseInt(tfArticle.getText().trim()));
            p.setStock_quantity(Integer.parseInt(tfStock.getText().trim()));
            p.setCategory(tfCategory.getText());
            p.setDescription(taDescription.getText());

            String error = p.getValidationError();
            if (error != null) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка валидации: " + error,
                        "Неверные данные",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            this.result = p;
            this.okPressed = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка преобразования числа: " + ex.getMessage(),
                    "Неверный ввод",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        this.okPressed = false;
        this.result = null;
        dispose();
    }
}
