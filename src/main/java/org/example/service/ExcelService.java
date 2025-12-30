package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Sorteio;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelService {
    public List<Sorteio> lerResultados(String caminho) throws Exception {
        List<Sorteio> sorteios = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(caminho);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || row.getCell(0) == null) continue;
                try {
                    int concurso = (int) getValorNumerico(row.getCell(0));
                    LocalDate data = getValorData(row.getCell(1));
                    List<Integer> numeros = new ArrayList<>();
                    for (int j = 2; j <= 7; j++) numeros.add((int) getValorNumerico(row.getCell(j)));
                    sorteios.add(new Sorteio(concurso, data, numeros));
                } catch (Exception e) { System.err.println("Erro linha " + (i+1)); }
            }
        }
        return sorteios;
    }

    private double getValorNumerico(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        try { return Double.parseDouble(cell.getStringCellValue().trim().replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    private LocalDate getValorData(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
            return cell.getLocalDateTimeCellValue().toLocalDate();
        try { return LocalDate.parse(cell.getStringCellValue().trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception e) { return LocalDate.now(); }
    }
}