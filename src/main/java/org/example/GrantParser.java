package org.example;

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

        // 1. Мәтінді тазалау: артық тырнақшаларды және бос орындарды реттеу
        String cleanText = text.replace("\"", " ").replace("\r", "");

        // 2. Мамандықтар бойынша мәтінді блоктарға бөлеміз
        // Мамандық коды: латын немесе кирилл "М" әрпі + 3 сан
        String[] specBlocks = cleanText.split("(?=[MМ]\\d{3}\\s*-\\s*)");

        // Студентті анықтайтын паттерн:
        // №(сан) + ТЖК(8 сан) + ФИО(мәтін) + Балл(сан) + ВУЗ(3 сан)
        // [\\s\\S]+? — бұл ФИО-ның бірнеше жолға созылуына мүмкіндік береді (ленивый поиск)
        Pattern studentPattern = Pattern.compile("(\\d+)\\s+(\\d{8})\\s+([A-ZА-ЯӘІҢҒҮҰҚӨҺ\\s\\n\\-]+?)\\s+(\\d{2,3})\\s+(\\d{3})");

        for (String block : specBlocks) {
            if (block.trim().isEmpty()) continue;

            // Мамандық атын алу
            String specCode = "Белгісіз";
            String specName = "Белгісіз";
            Pattern specInfoPattern = Pattern.compile("^([MМ]\\d{3})\\s*-\\s*([^\\n]+)");
            Matcher specMatcher = specInfoPattern.matcher(block);

            if (specMatcher.find()) {
                specCode = specMatcher.group(1).replace("М", "M"); // Латынша M-ге айналдыру
                specName = specMatcher.group(2).trim();
            }

            // Блок ішінен барлық студенттерді іздеу
            Matcher stMatcher = studentPattern.matcher(block);
            while (stMatcher.find()) {
                String order = stMatcher.group(1);
                String tjk = stMatcher.group(2);
                // ФИО ішіндегі артық жол ауыстыру белгілерін жою
                String fullName = stMatcher.group(3).replaceAll("\\s+", " ").trim();
                String score = stMatcher.group(4);
                String uniCode = stMatcher.group(5);

                // Тақырыптарды (№, ТЖК) студент ретінде алмас үшін тексеру
                if (!fullName.isEmpty() && !fullName.contains("Тегі")) {
                    students.add(new Student(specCode, specName, order, tjk, fullName, score, uniCode));
                }
            }
        }
        return students;
    }
}