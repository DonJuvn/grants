package org.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrantParser {

    public static class Student {
        String specCode, specName, order, tjk, fullName, score, universityCode, level;

        public Student(String specCode, String specName, String order, String tjk, String fullName, String score, String universityCode, String level) {
            this.specCode = specCode + " (" + level + ")";
            this.specName = specName;
            this.order = order;
            this.tjk = tjk;
            this.fullName = fullName;
            this.score = score;
            this.universityCode = universityCode;
            this.level = level;
        }

        public String[] toCsvRow() {
            return new String[]{specCode, specName, order, tjk, fullName, score, universityCode};
        }
    }

    public static List<Student> parseText(String text) {
        List<Student> students = new ArrayList<>();
        // Очищаем текст от кавычек [cite: 6, 10, 12]
        String cleanText = text.replace("\"", " ").replace("\r", "");
        String[] lines = cleanText.split("\n");

        String currentSpecCode = "Белгісіз";
        String currentSpecName = "Белгісіз";
        String currentLevel = "Бакалавриат";

        Pattern specPattern = Pattern.compile("([MМ]\\d{3})\\s*-\\s*(.*)");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // 1. Определение уровня обучения [cite: 4]
            if (line.toUpperCase().contains("МАГИСТРАТУРА")) {
                currentLevel = "Магистратура";
                continue;
            } else if (line.toUpperCase().contains("БАКАЛАВРИАТ")) {
                currentLevel = "Бакалавриат";
                continue;
            }

            // 2. Определение специальности [cite: 5, 7, 9, 11]
            Matcher specMatcher = specPattern.matcher(line);
            if (specMatcher.find()) {
                currentSpecCode = specMatcher.group(1).replace("М", "M");
                currentSpecName = specMatcher.group(2).trim();
                continue;
            }

            // 3. Сбор данных студента (Логика поглощения строк)
            // Проверяем: начинается ли строка с Номера и ТЖК (8 цифр) [cite: 6, 10, 12]
            if (line.matches("^\\d+\\s+\\d{8}.*")) {
                String[] firstLineParts = line.split("\\s+");
                String order = firstLineParts[0];
                String tjk = firstLineParts[1];

                StringBuilder fullNameBuilder = new StringBuilder();
                // Добавляем то, что осталось в первой строке после ТЖК
                for (int k = 2; k < firstLineParts.length; k++) {
                    fullNameBuilder.append(firstLineParts[k]).append(" ");
                }

                String score = "";
                String uniCode = "";

                // Идем по следующим строкам, пока не найдем Балл и ВУЗ
                int j = i;
                // Если в первой строке уже были балл и вуз (две группы цифр в конце)
                if (line.matches(".*\\d{1,3}\\s+\\d{3}$")) {
                    score = firstLineParts[firstLineParts.length - 2];
                    uniCode = firstLineParts[firstLineParts.length - 1];
                    // Убираем их из ФИО
                    String tempName = fullNameBuilder.toString().trim();
                    fullNameBuilder = new StringBuilder(tempName.substring(0, tempName.lastIndexOf(score)).trim());
                } else {
                    // Ищем в следующих строках
                    while (++j < lines.length) {
                        String nextLine = lines[j].trim();
                        if (nextLine.isEmpty()) continue;

                        if (nextLine.matches(".*\\d{1,3}\\s+\\d{3}$")) {
                            String[] nextLineParts = nextLine.split("\\s+");
                            // Все до последних двух чисел — это остаток ФИО
                            for (int k = 0; k < nextLineParts.length - 2; k++) {
                                fullNameBuilder.append(nextLineParts[k]).append(" ");
                            }
                            score = nextLineParts[nextLineParts.length - 2];
                            uniCode = nextLineParts[nextLineParts.length - 1];
                            i = j; // Сдвигаем основной цикл
                            break;
                        } else {
                            // Если строка не содержит баллы, значит это всё ФИО
                            fullNameBuilder.append(nextLine).append(" ");
                        }
                    }
                }

                if (!score.isEmpty()) {
                    // Авто-переключение уровня при сбросе нумерации и высоких баллах
                    if (order.equals("1") && Integer.parseInt(score) > 100) {
                        currentLevel = "Бакалавриат";
                    }

                    students.add(new Student(
                            currentSpecCode,
                            currentSpecName,
                            order,
                            tjk,
                            fullNameBuilder.toString().replaceAll("\\s+", " ").trim(),
                            score,
                            uniCode,
                            currentLevel
                    ));
                }
            }
        }
        return students;
    }
}