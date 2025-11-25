package gui;

import javax.swing.*;
import java.awt.*;

public class SearchDialog extends JDialog {

    public static class Result {
        public final String fieldName;
        public final String value;

        public Result(String fieldName, String value) {
            this.fieldName = fieldName;
            this.value = value;
        }
    }

    private JComboBox<String> cbField;
    private JTextField tfValue;
    private boolean okPressed = false;
    private Result result;

    // ВАЖНО: теперь owner = Frame, а не Window
    public static Result showDialog(Frame owner, String title, String[] fields) {
        SearchDialog dlg = new SearchDialog(owner, title, fields);
        dlg.setVisible(true);
        return dlg.okPressed ? dlg.result : null;
    }

    // тоже Frame и корректный super(...)
    private SearchDialog(Frame owner, String title, String[] fields) {
        super(owner, title, true);  // modal = true
        cbField = new JComboBox<>(fields);
        tfValue = new JTextField(20);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Поле:"));
        form.add(cbField);
        form.add(new JLabel("Значение:"));
        form.add(tfValue);

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

        pack();
        setLocationRelativeTo(owner);
    }

    private void onOk() {
        String field = (String) cbField.getSelectedItem();
        String value = tfValue.getText();
        if (field == null || value == null || value.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Заполните значение.",
                    "Неверный ввод",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.result = new Result(field, value.trim());
        this.okPressed = true;
        dispose();
    }

    private void onCancel() {
        this.okPressed = false;
        this.result = null;
        dispose();
    }
}
