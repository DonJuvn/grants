    package org.example;

    import org.apache.pdfbox.pdmodel.*;
    import org.apache.pdfbox.pdmodel.common.PDRectangle;
    import org.apache.pdfbox.pdmodel.font.PDType0Font;
    import org.apache.pdfbox.text.PDFTextStripper;

    import java.io.File;
    import java.io.IOException;
    import java.util.*;
    import java.util.stream.Collectors;

    public class Main {

        public static void main(String[] args) {

            String inputPdf = "grant.pdf";
            String outputPdf = "grants-result.pdf";

            File pdfFile = new File(inputPdf);
            if (!pdfFile.exists()) {
                System.err.println("Файл не найден: " + pdfFile.getAbsolutePath());
                return;
            }

            try (PDDocument document = PDDocument.load(pdfFile)) {

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                List<GrantParser.Student> students =
                        GrantParser.parseText(text);

                saveToPdf(students, outputPdf);

                System.out.println("PDF успешно создан: " + outputPdf);

                search(students);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // ================= SAVE GROUPED TABLES =================

        private static void saveToPdf(List<GrantParser.Student> students,
                                      String outputPath) throws IOException {

            PDDocument doc = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            File fontFile = new File("C:/Windows/Fonts/arial.ttf");
            PDType0Font font = PDType0Font.load(doc, fontFile);

            PDPageContentStream content =
                    new PDPageContentStream(doc, page);

            float margin = 40;
            float y = 800;
            float rowHeight = 18;

            float[] colWidths = {40, 90, 250, 50, 50};
            String[] headers = {"№", "ТЖК", "ФИО", "Балл", "ВУЗ"};

            Map<String, List<GrantParser.Student>> grouped =
                    students.stream()
                            .collect(Collectors.groupingBy(
                                    s -> s.specCode + " - " + s.specName,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            for (Map.Entry<String, List<GrantParser.Student>> entry : grouped.entrySet()) {

                // Если мало места — новая страница
                if (y < 100) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    content = new PDPageContentStream(doc, page);
                    y = 800;
                }

                // ==== Заголовок специальности ====
                content.setFont(font, 14);
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText(entry.getKey());
                content.endText();

                y -= 25;

                content.setFont(font, 9);

                // Заголовок таблицы
                y = drawRow(content, font, headers,
                        colWidths, margin, y, rowHeight);

                for (GrantParser.Student s : entry.getValue()) {

                    if (y < 60) {
                        content.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        content = new PDPageContentStream(doc, page);
                        content.setFont(font, 9);
                        y = 800;

                        y = drawRow(content, font, headers,
                                colWidths, margin, y, rowHeight);
                    }

                    String[] row = {
                            s.order,
                            s.tjk,
                            s.fullName,
                            s.score,
                            s.universityCode
                    };

                    y = drawRow(content, font, row,
                            colWidths, margin, y, rowHeight);
                }

                y -= 20; // Отступ перед следующей специальностью
            }

            content.close();
            doc.save(outputPath);
            doc.close();
        }


        // ================= DRAW ROW =================

        private static float drawRow(PDPageContentStream content,
                                     PDType0Font font,
                                     String[] cells,
                                     float[] colWidths,
                                     float margin,
                                     float y,
                                     float rowHeight) throws IOException {

            float x = margin;

            for (int i = 0; i < cells.length; i++) {

                content.addRect(x, y - rowHeight, colWidths[i], rowHeight);
                content.stroke();

                content.beginText();
                content.newLineAtOffset(x + 3, y - 12);
                content.showText(cells[i] == null ? "" : cells[i]);
                content.endText();

                x += colWidths[i];
            }

            return y - rowHeight;
        }

        // ================= SEARCH =================

        private static void search(List<GrantParser.Student> students) {

            Scanner sc = new Scanner(System.in);
            System.out.println("\nВведите код специальности (например, M066) или 'exit':");

            while (true) {

                System.out.print("> ");
                String code = sc.nextLine().trim();

                if (code.equalsIgnoreCase("exit"))
                    break;

                students.stream()
                        .filter(s -> s.specCode.equalsIgnoreCase(code))
                        .max(Comparator.comparingInt(
                                s -> Integer.parseInt(s.score)))
                        .ifPresentOrElse(
                                s -> System.out.printf(
                                        "Лучший студент: %s | %s | Балл: %s | ВУЗ: %s%n",
                                        s.specCode,
                                        s.fullName,
                                        s.score,
                                        s.universityCode
                                ),
                                () -> System.out.println("Специальность не найдена.")
                        );
            }
        }
    }
