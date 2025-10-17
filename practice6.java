import java.util.*;
import java.util.concurrent.*;

interface ICostCalculationStrategy {
    double calculateCost(double distance, String serviceClass, int passengers, boolean hasLuggage, boolean isChildOrSenior);
}

class AirplaneStrategy implements ICostCalculationStrategy {
    public double calculateCost(double distance, String serviceClass, int passengers, boolean hasLuggage, boolean isChildOrSenior) {
        double baseRate = 0.3;
        double classMultiplier = serviceClass.equalsIgnoreCase("бизнес") ? 2.5 : 1.0;
        double cost = distance * baseRate * classMultiplier * passengers;
        if (hasLuggage) cost += 25 * passengers;
        if (isChildOrSenior) cost *= 0.85;
        return cost + 50; // аэропортовый сбор
    }
}

class TrainStrategy implements ICostCalculationStrategy {
    public double calculateCost(double distance, String serviceClass, int passengers, boolean hasLuggage, boolean isChildOrSenior) {
        double baseRate = 0.12;
        double classMultiplier = serviceClass.equalsIgnoreCase("бизнес") ? 1.8 : 1.0;
        double cost = distance * baseRate * classMultiplier * passengers;
        if (hasLuggage) cost += 10 * passengers;
        if (isChildOrSenior) cost *= 0.75;
        return cost;
    }
}

class BusStrategy implements ICostCalculationStrategy {
    public double calculateCost(double distance, String serviceClass, int passengers, boolean hasLuggage, boolean isChildOrSenior) {
        double baseRate = 0.08;
        double classMultiplier = serviceClass.equalsIgnoreCase("бизнес") ? 1.5 : 1.0;
        double cost = distance * baseRate * classMultiplier * passengers;
        if (hasLuggage) cost += 5 * passengers;
        if (isChildOrSenior) cost *= 0.8;
        return cost;
    }
}

class TravelBookingContext {
    private ICostCalculationStrategy strategy;

    public void setStrategy(ICostCalculationStrategy strategy) {
        this.strategy = strategy;
    }

    public double calculateTotalCost(double distance, String serviceClass, int passengers, boolean hasLuggage, boolean isChildOrSenior) {
        if (strategy == null) throw new IllegalStateException("Стратегия не установлена.");
        if (distance <= 0 || passengers <= 0) throw new IllegalArgumentException("Некорректные входные данные.");
        return strategy.calculateCost(distance, serviceClass, passengers, hasLuggage, isChildOrSenior);
    }
}

interface IObserver {
    void update(String stockSymbol, double newPrice);
    Set<String> getSubscribedStocks();
}

interface ISubject {
    void registerObserver(IObserver observer, String stockSymbol);
    void removeObserver(IObserver observer, String stockSymbol);
    void notifyObservers(String stockSymbol, double newPrice);
    void setStockPrice(String stockSymbol, double price);
}

class StockExchange implements ISubject {
    private Map<String, Double> stockPrices = new HashMap<>();
    private Map<String, Set<IObserver>> observersByStock = new HashMap<>();
    private ExecutorService executor = Executors.newCachedThreadPool();

    public void registerObserver(IObserver observer, String stockSymbol) {
        observersByStock.computeIfAbsent(stockSymbol, k -> new HashSet<>()).add(observer);
        System.out.println("Наблюдатель подписан на акцию: " + stockSymbol);
    }

    public void removeObserver(IObserver observer, String stockSymbol) {
        Set<IObserver> observers = observersByStock.get(stockSymbol);
        if (observers != null) {
            observers.remove(observer);
            System.out.println("Наблюдатель отписан от акции: " + stockSymbol);
        }
    }

    public void notifyObservers(String stockSymbol, double newPrice) {
        Set<IObserver> observers = observersByStock.get(stockSymbol);
        if (observers != null) {
            for (IObserver observer : observers) {
                if (observer.getSubscribedStocks().contains(stockSymbol)) {
                    executor.submit(() -> observer.update(stockSymbol, newPrice));
                }
            }
        }
    }

    public void setStockPrice(String stockSymbol, double price) {
        if (price < 0) {
            System.out.println("Ошибка: цена акции не может быть отрицательной.");
            return;
        }
        stockPrices.put(stockSymbol, price);
        System.out.println("Цена акции " + stockSymbol + " обновлена: " + price);
        notifyObservers(stockSymbol, price);
    }

    public void shutdown() {
        executor.shutdown();
    }
}

class TraderObserver implements IObserver {
    private String name;
    private Set<String> subscribedStocks = new HashSet<>();

    public TraderObserver(String name) {
        this.name = name;
    }

    public void update(String stockSymbol, double newPrice) {
        System.out.println("[Трейдер " + name + "] Акция " + stockSymbol + " теперь стоит: " + newPrice);
    }

    public Set<String> getSubscribedStocks() {
        return subscribedStocks;
    }

    public void subscribeTo(String stock) {
        subscribedStocks.add(stock);
    }
}

class TradingBotObserver implements IObserver {
    private String name;
    private Set<String> subscribedStocks = new HashSet<>();
    private Map<String, Double> thresholds = new HashMap<>();

    public TradingBotObserver(String name) {
        this.name = name;
    }

    public void setThreshold(String stock, double threshold) {
        thresholds.put(stock, threshold);
    }

    public void update(String stockSymbol, double newPrice) {
        Double threshold = thresholds.get(stockSymbol);
        if (threshold != null) {
            if (newPrice > threshold) {
                System.out.println("[Робот " + name + "] ПОКУПКА акции " + stockSymbol + " по цене " + newPrice + " (порог: " + threshold + ")");
            } else if (newPrice < threshold * 0.9) {
                System.out.println("[Робот " + name + "] ПРОДАЖА акции " + stockSymbol + " по цене " + newPrice);
            }
        }
    }

    public Set<String> getSubscribedStocks() {
        return subscribedStocks;
    }

    public void subscribeTo(String stock) {
        subscribedStocks.add(stock);
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Система бронирования путешествий
        System.out.println("Система бронирования путешествий");
        System.out.println("Выберите транспорт:");
        System.out.println("1 - Самолёт");
        System.out.println("2 - Поезд");
        System.out.println("3 - Автобус");
        String transportChoice = scanner.nextLine();

        TravelBookingContext bookingContext = new TravelBookingContext();
        switch (transportChoice) {
            case "1":
                bookingContext.setStrategy(new AirplaneStrategy());
                break;
            case "2":
                bookingContext.setStrategy(new TrainStrategy());
                break;
            case "3":
                bookingContext.setStrategy(new BusStrategy());
                break;
            default:
                System.out.println("Неверный выбор транспорта.");
                scanner.close();
                return;
        }

        try {
            System.out.println("Введите расстояние (км):");
            double distance = Double.parseDouble(scanner.nextLine());
            System.out.println("Класс обслуживания (эконом/бизнес):");
            String serviceClass = scanner.nextLine().toLowerCase();
            if (!serviceClass.equals("эконом") && !serviceClass.equals("бизнес")) {
                System.out.println("Некорректный класс обслуживания.");
                scanner.close();
                return;
            }
            System.out.println("Количество пассажиров:");
            int passengers = Integer.parseInt(scanner.nextLine());
            System.out.println("Есть багаж? (да/нет):");
            boolean hasLuggage = scanner.nextLine().trim().toLowerCase().startsWith("д");
            System.out.println("Есть дети или пенсионеры? (да/нет):");
            boolean isChildOrSenior = scanner.nextLine().trim().toLowerCase().startsWith("д");

            double totalCost = bookingContext.calculateTotalCost(distance, serviceClass, passengers, hasLuggage, isChildOrSenior);
            System.out.printf("Итоговая стоимость поездки: %.2f руб.\n", totalCost);
        } catch (Exception e) {
            System.out.println("Ошибка ввода данных: " + e.getMessage());
            scanner.close();
            return;
        }

        System.out.println();

        // Система биржевых торгов
        System.out.println("Система биржевых торгов");
        StockExchange exchange = new StockExchange();

        TraderObserver trader1 = new TraderObserver("Сумая");
        trader1.subscribeTo("AAPL");
        trader1.subscribeTo("GOOGL");

        TradingBotObserver bot1 = new TradingBotObserver("SuperBot");
        bot1.subscribeTo("AAPL");
        bot1.setThreshold("AAPL", 190.0);
        bot1.subscribeTo("TSLA");
        bot1.setThreshold("TSLA", 250.0);

        exchange.registerObserver(trader1, "AAPL");
        exchange.registerObserver(trader1, "GOOGL");
        exchange.registerObserver(bot1, "AAPL");
        exchange.registerObserver(bot1, "TSLA");

        System.out.println("\n--- Изменение цен акций ---");
        exchange.setStockPrice("AAPL", 185.0);
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        exchange.setStockPrice("AAPL", 192.0);
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        exchange.setStockPrice("TSLA", 240.0);
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        exchange.setStockPrice("TSLA", 260.0);

        System.out.println("\nОтписка трейдера от AAPL");
        exchange.removeObserver(trader1, "AAPL");
        exchange.setStockPrice("AAPL", 195.0);

        scanner.close();
        exchange.shutdown();
    }
}