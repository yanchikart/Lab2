package model;

import java.util.Objects;

public class Product {
    private int product_id;
    private int seller_id;
    private String name;
    private String description;
    private double price;
    private String brand;
    private int article;
    private int stock_quantity;
    private String category;

    public Product() {
    }

    public Product(int product_id, int seller_id, String name, String price, String brand, int article, int stock_quantity, String category, String description) {
        this.product_id = product_id;
        this.seller_id = seller_id;
        this.name = name;
        this.price = Double.parseDouble(price);
        this.brand = brand;
        this.article = article;
        this.stock_quantity = stock_quantity;
        this.category = category;
        this.description = description;

    }

    public int getProduct_id() {
        return product_id;
    }

    public void setProduct_id(int product_id) {
        this.product_id = product_id;
    }

    public int getSeller_id() {
        return seller_id;
    }

    public void setSeller_id(int seller_id) {
        this.seller_id = seller_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getArticle() {
        return article;
    }

    public void setArticle(int article) {
        this.article = article;
    }

    public int getStock_quantity() {
        return stock_quantity;
    }

    public void setStock_quantity(int stock_quantity) {
        this.stock_quantity = stock_quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object[] CreatingTable() {
        return new Object[]{
                product_id,
                seller_id,
                name,
                price,
                brand,
                article,
                stock_quantity,
                category,
                description
        };
    }

    @Override
    public String toString() {
        return String.format("""
                Product {
                    id=%d,
                    name='%s',
                    price=%.2f,
                    stock=%d,
                    category='%s',
                    article='%s',
                    brand='%s',
                    sellerId=%d,
                    description='%s'
                }""", product_id, name, price, stock_quantity, category, article, brand, seller_id, description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return product_id == product.product_id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(product_id);
    }




    public String getValidationError() {
        if (product_id <= 0) return "ID товара должен быть положительным числом";
        if (name == null || name.trim().isEmpty()) return "Название товара не может быть пустым";
        if (price < 0) return "Цена не может быть отрицательной";
        if (stock_quantity < 0) return "Количество не может быть отрицательным";
        if (category == null) return "Категория не может быть null";
        if (article <= 0) return "Артикул должен быть положительным";
        if (brand == null) return "Бренд не может быть null";
        if (seller_id <= 0) return "ID продавца должен быть положительным числом";
        return null; // нет ошибок
    }
}