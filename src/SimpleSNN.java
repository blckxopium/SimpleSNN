import java.util.Random;

/**
 * ПРОСТАЯ СПАЙКОВАЯ НЕЙРОННАЯ СЕТЬ
 * Лабораторная работа №3: Нейроморфный интеллект
 * 
 * Демонстрация:
 * 1. LIF-нейрон (Leaky Integrate-and-Fire)
 * 2. STDP-обучение (Spike-Timing-Dependent Plasticity)
 * 3. Простая классификация паттернов
 */
public class SimpleSNN {
    
    static Random random = new Random();
    
    // ========================================================================
    // LIF-НЕЙРОН: "Ведро с дыркой"
    // ========================================================================
    
    static class LIFNeuron {
        double potential = 0.0;      // Мембранный потенциал ("уровень воды")
        double threshold = 1.0;       // Порог срабатывания ("край ведра")
        double decay = 0.9;           // Утечка ("дырка в ведре")
        boolean spiked = false;       // Был ли спайк
        
        /**
         * Обновление нейрона
         * @param input - входной сигнал
         * @return true если произошёл спайк
         */
        boolean update(double input) {
            // 1. Утечка: потенциал уменьшается со временем
            potential = potential * decay;
            
            // 2. Накопление: добавляем входной сигнал
            potential = potential + input;
            
            // 3. Проверка порога: если "ведро переполнилось"
            if (potential >= threshold) {
                potential = 0.0;  // Сброс ("ведро опрокинулось")
                spiked = true;
                return true;      // Спайк!
            }
            
            spiked = false;
            return false;
        }
        
        void reset() {
            potential = 0.0;
            spiked = false;
        }
    }
    
    // ========================================================================
    // STDP-СИНАПС: "Кто первый - тот причина"
    // ========================================================================
    
    static class Synapse {
        double weight;  // Сила связи между нейронами
        
        Synapse(double w) {
            this.weight = w;
        }
        
        /**
         * STDP-обучение
         * @param preSpiked - сработал ли пресинаптический нейрон
         * @param postSpiked - сработал ли постсинаптический нейрон
         */
        void stdpUpdate(boolean preSpiked, boolean postSpiked) {
            if (preSpiked && postSpiked) {
                // Оба сработали -> усиливаем связь (LTP)
                weight = Math.min(1.0, weight + 0.1);
            } else if (preSpiked && !postSpiked) {
                // Только пре сработал -> немного ослабляем (LTD)
                weight = Math.max(0.0, weight - 0.05);
            }
        }
    }
    
    // ========================================================================
    // ПРОСТАЯ СЕТЬ: 4 входа -> 2 выхода
    // ========================================================================
    
    static class SimpleNetwork {
        // 4 входных нейрона, 2 выходных
        LIFNeuron[] inputNeurons = new LIFNeuron[4];
        LIFNeuron[] outputNeurons = new LIFNeuron[2];
        
        // Синапсы: каждый вход связан с каждым выходом (4 x 2 = 8 связей)
        Synapse[][] synapses = new Synapse[4][2];
        
        SimpleNetwork() {
            // Создаём нейроны
            for (int i = 0; i < 4; i++) {
                inputNeurons[i] = new LIFNeuron();
            }
            for (int i = 0; i < 2; i++) {
                outputNeurons[i] = new LIFNeuron();
            }
            
            // Создаём синапсы со случайными весами
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 2; j++) {
                    synapses[i][j] = new Synapse(0.3 + random.nextDouble() * 0.4);
                }
            }
        }
        
        /**
         * Прямой проход: подаём данные, получаем ответ
         * @param inputs - 4 входных значения
         * @return номер выходного нейрона с наибольшей активностью
         */
        int forward(double[] inputs, boolean learn) {
            // Сброс нейронов
            for (LIFNeuron n : inputNeurons) n.reset();
            for (LIFNeuron n : outputNeurons) n.reset();
            
            int[] outputSpikes = new int[2];  // Счётчик спайков
            
            // Симуляция на 20 шагов времени
            for (int t = 0; t < 20; t++) {
                
                // 1. Обновляем входные нейроны
                for (int i = 0; i < 4; i++) {
                    inputNeurons[i].update(inputs[i] * 0.5);
                }
                
                // 2. Вычисляем ток для выходных нейронов
                double[] outputCurrent = new double[2];
                for (int i = 0; i < 4; i++) {
                    if (inputNeurons[i].spiked) {
                        for (int j = 0; j < 2; j++) {
                            outputCurrent[j] += synapses[i][j].weight;
                        }
                    }
                }
                
                // 3. Обновляем выходные нейроны
                for (int j = 0; j < 2; j++) {
                    if (outputNeurons[j].update(outputCurrent[j])) {
                        outputSpikes[j]++;
                    }
                }
                
                // 4. STDP-обучение
                if (learn) {
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 2; j++) {
                            synapses[i][j].stdpUpdate(
                                inputNeurons[i].spiked,
                                outputNeurons[j].spiked
                            );
                        }
                    }
                }
            }
            
            // Возвращаем нейрон с большим числом спайков
            return (outputSpikes[0] >= outputSpikes[1]) ? 0 : 1;
        }
        
        /**
         * Обучение с подкреплением
         */
        void trainWithReward(double[] inputs, int correctClass) {
            int predicted = forward(inputs, true);
            
            if (predicted == correctClass) {
                // Правильно! Усиливаем связи с правильным выходом
                for (int i = 0; i < 4; i++) {
                    synapses[i][correctClass].weight = 
                        Math.min(1.0, synapses[i][correctClass].weight + 0.05);
                }
            } else {
                // Неправильно! Ослабляем связи с неправильным выходом
                for (int i = 0; i < 4; i++) {
                    synapses[i][predicted].weight = 
                        Math.max(0.0, synapses[i][predicted].weight - 0.05);
                }
            }
        }
        
        void printWeights() {
            System.out.println("\nВеса синапсов:");
            System.out.println("         Выход 0   Выход 1");
            for (int i = 0; i < 4; i++) {
                System.out.printf("Вход %d:   %.3f     %.3f%n", 
                    i, synapses[i][0].weight, synapses[i][1].weight);
            }
        }
    }
    
    // ========================================================================
    // ДЕМОНСТРАЦИЯ LIF-НЕЙРОНА
    // ========================================================================
    
    static void demoLIF() {
        System.out.println("=".repeat(60));
        System.out.println("ДЕМОНСТРАЦИЯ LIF-НЕЙРОНА");
        System.out.println("=".repeat(60));
        System.out.println("\nLIF = Leaky Integrate-and-Fire (Интегрирующий с утечкой)");
        System.out.println("Аналогия: ВЕДРО С ДЫРКОЙ");
        System.out.println("- Вода наливается (входной сигнал)");
        System.out.println("- Вода вытекает через дырку (утечка)");
        System.out.println("- Когда переполняется - опрокидывается (СПАЙК!)");
        
        LIFNeuron neuron = new LIFNeuron();
        
        System.out.println("\nПодаём постоянный сигнал 0.3:");
        System.out.println("Шаг | Потенциал | Спайк");
        System.out.println("----|-----------|------");
        
        for (int t = 1; t <= 15; t++) {
            boolean spike = neuron.update(0.3);
            System.out.printf("%3d |   %.3f   |  %s%n", 
                t, neuron.potential, spike ? "СПАЙК!" : "");
        }
    }
    
    // ========================================================================
    // ДЕМОНСТРАЦИЯ STDP
    // ========================================================================
    
    static void demoSTDP() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ДЕМОНСТРАЦИЯ STDP");
        System.out.println("=".repeat(60));
        System.out.println("\nSTDP = Spike-Timing-Dependent Plasticity");
        System.out.println("Правило: КТО ПЕРВЫЙ - ТОТ ПРИЧИНА");
        System.out.println("- Нейрон A сработал ДО нейрона B -> усиливаем связь A->B");
        System.out.println("- Нейрон A сработал ПОСЛЕ нейрона B -> ослабляем связь");
        
        Synapse syn = new Synapse(0.5);
        
        System.out.println("\nНачальный вес: " + syn.weight);
        
        System.out.println("\nСценарий 1: Оба нейрона сработали вместе (LTP)");
        syn.stdpUpdate(true, true);
        System.out.println("Вес после: " + syn.weight);
        
        System.out.println("\nСценарий 2: Только пре-нейрон сработал (LTD)");
        syn.stdpUpdate(true, false);
        System.out.println("Вес после: " + syn.weight);
        
        System.out.println("\nСценарий 3: Никто не сработал (без изменений)");
        syn.stdpUpdate(false, false);
        System.out.println("Вес после: " + syn.weight);
    }
    
    // ========================================================================
    // ДЕМОНСТРАЦИЯ КЛАССИФИКАЦИИ
    // ========================================================================
    
    static void demoClassification() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ДЕМОНСТРАЦИЯ КЛАССИФИКАЦИИ");
        System.out.println("=".repeat(60));
        
        System.out.println("\nЗадача: различить два типа паттернов");
        System.out.println("Класс 0: высокие значения слева  [1,1,0,0]");
        System.out.println("Класс 1: высокие значения справа [0,0,1,1]");
        
        // Данные для обучения
        double[][] patterns = {
            {1.0, 0.8, 0.1, 0.2},  // Класс 0
            {0.9, 1.0, 0.2, 0.1},  // Класс 0
            {0.8, 0.9, 0.0, 0.1},  // Класс 0
            {0.1, 0.2, 1.0, 0.8},  // Класс 1
            {0.2, 0.1, 0.9, 1.0},  // Класс 1
            {0.0, 0.1, 0.8, 0.9},  // Класс 1
        };
        int[] labels = {0, 0, 0, 1, 1, 1};
        
        SimpleNetwork network = new SimpleNetwork();
        
        System.out.println("\n--- ДО ОБУЧЕНИЯ ---");
        network.printWeights();
        
        int correctBefore = 0;
        for (int i = 0; i < patterns.length; i++) {
            int pred = network.forward(patterns[i], false);
            if (pred == labels[i]) correctBefore++;
        }
        System.out.printf("\nТочность до обучения: %d/%d (%.0f%%)%n", 
            correctBefore, patterns.length, 100.0 * correctBefore / patterns.length);
        
        // Обучение
        System.out.println("\n--- ОБУЧЕНИЕ (50 эпох) ---");
        for (int epoch = 0; epoch < 50; epoch++) {
            for (int i = 0; i < patterns.length; i++) {
                network.trainWithReward(patterns[i], labels[i]);
            }
            
            if ((epoch + 1) % 10 == 0) {
                int correct = 0;
                for (int i = 0; i < patterns.length; i++) {
                    int pred = network.forward(patterns[i], false);
                    if (pred == labels[i]) correct++;
                }
                System.out.printf("Эпоха %2d: точность %d/%d%n", 
                    epoch + 1, correct, patterns.length);
            }
        }
        
        System.out.println("\n--- ПОСЛЕ ОБУЧЕНИЯ ---");
        network.printWeights();
        
        int correctAfter = 0;
        System.out.println("\nРезультаты классификации:");
        for (int i = 0; i < patterns.length; i++) {
            int pred = network.forward(patterns[i], false);
            boolean correct = (pred == labels[i]);
            if (correct) correctAfter++;
            System.out.printf("Паттерн %d: предсказано=%d, правильно=%d %s%n",
                i, pred, labels[i], correct ? "✓" : "✗");
        }
        System.out.printf("\nИтоговая точность: %d/%d (%.0f%%)%n", 
            correctAfter, patterns.length, 100.0 * correctAfter / patterns.length);
    }
    
    // ========================================================================
    // MAIN
    // ========================================================================
    
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ЛАБОРАТОРНАЯ РАБОТА №3: НЕЙРОМОРФНЫЙ ИНТЕЛЛЕКТ");
        System.out.println("Простая спайковая нейронная сеть с STDP");
        System.out.println("=".repeat(60));
        
        // 1. Демонстрация LIF-нейрона
        demoLIF();
        
        // 2. Демонстрация STDP
        demoSTDP();
        
        // 3. Демонстрация классификации
        demoClassification();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ПРОГРАММА ЗАВЕРШЕНА");
        System.out.println("=".repeat(60));
    }
}
