import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.opencsv.CSVWriter;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String pdfPath = "grant-1.pdf";
        String csvPath = "grants.csv";

        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            System.err.println("Файл не найден по пути: " + pdfFile.getAbsolutePath());
            return;
        }

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            List<GrantParser.Student> students = GrantParser.parseText(text);

            saveToCsv(students, csvPath);
            System.out.println("Готово! Извлечено студентов: " + students.size());
            System.out.println("Данные сохранены в: " + csvPath);

            search(students);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveToCsv(List<GrantParser.Student> students, String path) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(path))) {
            writer.writeNext(new String[]{"Код", "Специальность", "№", "ТЖК", "ФИО", "Балл", "ВУЗ"});
            for (GrantParser.Student s : students) {
                writer.writeNext(s.toCsvRow());
            }
        }
    }

    private static void search(List<GrantParser.Student> students) {
        Scanner sc = new Scanner(System.in);
        System.out.println("\nВведите код специальности (например, М066) или 'exit' для выхода:");

        while (true) {
            System.out.print("> ");
            String code = sc.nextLine().trim();
            if (code.equalsIgnoreCase("exit")) break;

            students.stream()
                    .filter(s -> s.specCode.equalsIgnoreCase(code))
                    .max(Comparator.comparingInt(s -> Integer.parseInt(s.score)))
                    .ifPresentOrElse(
                            s -> System.out.printf("Результат: %s | %s | Балл: %s | ВУЗ: %s%n",
                                    s.specCode, s.fullName, s.score, s.universityCode),
                            () -> System.out.println("Специальность не найдена.")
                    );
        }
    }
}