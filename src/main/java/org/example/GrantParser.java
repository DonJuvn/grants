import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrantParser {

    public static class Student {
        String specCode, specName, order, tjk, fullName, score, universityCode;

        public Student(String specCode, String specName, String order, String tjk, String fullName, String score, String universityCode) {
            this.specCode = specCode;
            this.specName = specName;
            this.order = order;
            this.tjk = tjk;
            this.fullName = fullName;
            this.score = score;
            this.universityCode = universityCode;
        }

        public String[] toCsvRow() {
            return new String[]{specCode, specName, order, tjk, fullName, score, universityCode};
        }
    }

    public static List<Student> parseText(String text) {
        List<Student> students = new ArrayList<>();

        // 1. Очистка: убираем кавычки и приводим переносы к одному виду
        String cleanText = text.replace("\"", "").replace("\r", "");
        String[] lines = cleanText.split("\n");

        String currentSpecCode = "Не указан";
        String currentSpecName = "Не указана";

        // Паттерн для поиска специальности (М + 3 цифры)
        // Ищем в строке что-то вроде "М056 - Название" или просто "М056"
        Pattern specPattern = Pattern.compile("(М\\d{3})\\s?[-–—]?\\s?(.*)");

        // Паттерн для строки студента (включая казахские буквы)
        Pattern studentPattern = Pattern.compile("^(\\d+)\\s+(\\d+.*?)\\s+([А-ЯЁA-ZӘІҢҒҮҰҚӨҺ\\s\\-]{3,})\\s+(\\d+)\\s+(\\d+)$");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.equalsIgnoreCase("№") || line.contains("ТЖК")) continue;

            // Сначала проверяем, не является ли строка новой специальностью
            Matcher specMatcher = specPattern.matcher(line);
            if (specMatcher.find()) {
                currentSpecCode = specMatcher.group(1).trim();
                String namePart = specMatcher.group(2).trim();
                if (!namePart.isEmpty()) {
                    currentSpecName = namePart;
                }
                continue; // Переходим к следующей строке
            }

            // Проверяем, является ли строка данными студента
            Matcher stMatcher = studentPattern.matcher(line);
            if (stMatcher.find()) {
                String order = stMatcher.group(1);
                String tjk = stMatcher.group(2).replace("|", "").trim();
                String name = stMatcher.group(3).trim();
                String score = stMatcher.group(4);
                String uni = stMatcher.group(5);

                students.add(new Student(currentSpecCode, currentSpecName, order, tjk, name, score, uni));
            }
        }
        return students;
    }
}