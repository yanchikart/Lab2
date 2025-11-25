package generator;

import dao.ProductDao;
import model.Product;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class DatabaseGenerator {

    private static final String[] CATEGORIES = {
            "Электроника", "Одежда", "Обувь", "Косметика", "Товары для дома",
            "Игрушки", "Спорт", "Автотовары", "Книги", "Продукты"
    };

    private static final String[] BRANDS = {
            "Samsung", "Nike", "Adidas", "Apple", "Xiaomi", "L'Oréal", "Bosch",
            "Sony", "Puma", "Reebok", "Huawei", "LG", "Philips", "Canon", "IKEA"
    };

    private static final String[] NAMES = {
            "Смартфон", "Кроссовки", "Телевизор", "Наушники", "Чехол", "Крем", "Лампа",
            "Футболка", "Куртка", "Ботинки", "Рюкзак", "Планшет", "Часы", "Пылесос", "Утюг"
    };

    private static final String[] DESCRIPTIONS = {
            "Высокое качество, оригинал, быстрая доставка.",
            "Новинка 2025 года! Гарантия 2 года.",
            "Стильный дизайн, удобная посадка.",
            "Подходит для повседневного использования.",
            "Экологичные материалы, гипоаллергенно.",
            "Произведено в России. Сертифицировано."
    };

    private static final Random rnd = new Random();

    public static void main(String[] args) {
        Path dataPath = Paths.get("wildberries_products.dat");
        Path indexPath = Paths.get("wildberries_products.idx");

        try (ProductDao db = new ProductDao(dataPath, indexPath)) {
            db.create();

            for (int i = 1; i <= 100; i++) {
                Product p = generateProduct(i);
                db.insert(p);
            }

            db.save();
            System.out.println("База данных с 100 товарами успешно создана:");
            System.out.println("   Данные: " + dataPath.toAbsolutePath());
            System.out.println("   Индекс: " + indexPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Product generateProduct(int id) {
        Product p = new Product();
        p.setProduct_id(id);
        p.setSeller_id(1000 + rnd.nextInt(9000)); // случайный ID продавца

        String nameBase = NAMES[rnd.nextInt(NAMES.length)];
        String brand = BRANDS[rnd.nextInt(BRANDS.length)];
        p.setName(brand + " " + nameBase + " " + (rnd.nextInt(50) + 1));

        p.setPrice(roundToTwoDecimals(500 + rnd.nextDouble() * 49500)); // 500–50,000 руб

        p.setBrand(brand);
        p.setArticle(100000 + rnd.nextInt(900000));

        p.setStock_quantity(rnd.nextInt(200) + 1); // 1–200 шт

        p.setCategory(CATEGORIES[rnd.nextInt(CATEGORIES.length)]);

        String desc = DESCRIPTIONS[rnd.nextInt(DESCRIPTIONS.length)];
        if (rnd.nextBoolean()) {
            desc += " " + generateExtraDescription();
        }
        p.setDescription(desc);

        return p;
    }

    private static double roundToTwoDecimals(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private static String generateExtraDescription() {
        String[] extras = {
                "Бесплатная доставка по России.",
                "Акция: скидка 15% при покупке 2 шт.",
                "В наличии в 5 цветах.",
                "Подарочная упаковка в комплекте.",
                "Гарантия возврата 30 дней."
        };
        return extras[rnd.nextInt(extras.length)];
    }
}